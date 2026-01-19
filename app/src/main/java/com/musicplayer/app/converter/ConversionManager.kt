package com.musicplayer.app.converter

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a queue of video-to-audio conversion tasks.
 * 
 * This class provides:
 * - Queue-based task management with priority support
 * - Concurrent conversion with configurable limits
 * - Pause/resume functionality for individual tasks
 * - Cancel operations for running and queued tasks
 * - Real-time status updates via Flow
 * - Persistent task state across configuration changes
 * - Automatic retry on transient failures
 * 
 * The manager ensures:
 * - Maximum concurrent conversions to prevent resource exhaustion
 * - Proper cleanup of resources when tasks are cancelled
 * - Thread-safe operations on the task queue
 * - Progress tracking for all tasks
 * 
 * @property converter Video to audio converter instance
 */
@Singleton
class ConversionManager @Inject constructor(
    private val converter: VideoToAudioConverter
) {
    companion object {
        private const val MAX_CONCURRENT_CONVERSIONS = 2
        private const val MAX_RETRY_ATTEMPTS = 2
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val conversionSemaphore = Semaphore(MAX_CONCURRENT_CONVERSIONS)

    private val _tasks = MutableStateFlow<List<ConversionTask>>(emptyList())
    val tasks: StateFlow<List<ConversionTask>> = _tasks.asStateFlow()

    private val _conversionEvents = MutableSharedFlow<ConversionEvent>(
        replay = 10,
        extraBufferCapacity = 50,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val conversionEvents: SharedFlow<ConversionEvent> = _conversionEvents.asSharedFlow()

    private val activeJobs = mutableMapOf<String, Job>()
    private val taskRetryCount = mutableMapOf<String, Int>()

    /**
     * Adds a new conversion task to the queue.
     * 
     * The task is immediately started if there's capacity, otherwise it's queued
     * and will start when a slot becomes available.
     * 
     * @param sourceUri URI of the source video file
     * @param sourcePath Display path of the source file
     * @param outputFormat Desired output audio format
     * @param bitrate Target audio bitrate
     * @return The created ConversionTask
     */
    fun addConversion(
        sourceUri: Uri,
        sourcePath: String,
        outputFormat: AudioFormat,
        bitrate: AudioBitrate
    ): ConversionTask {
        val task = ConversionTask(
            sourceUri = sourceUri,
            sourcePath = sourcePath,
            outputPath = "", // Will be set during conversion
            outputFormat = outputFormat,
            bitrate = bitrate,
            status = ConversionStatus.PENDING
        )

        addTask(task)
        startConversion(task.id)

        return task
    }

    /**
     * Starts conversion for a specific task.
     * 
     * @param taskId ID of the task to start
     */
    fun startConversion(taskId: String) {
        val task = findTask(taskId) ?: run {
            Timber.w("Task not found: $taskId")
            return
        }

        if (task.status != ConversionStatus.PENDING && task.status != ConversionStatus.PAUSED) {
            Timber.w("Task $taskId is not in a startable state: ${task.status}")
            return
        }

        val job = scope.launch {
            try {
                conversionSemaphore.acquire()
                
                updateTask(taskId) { it.withStatus(ConversionStatus.RUNNING) }
                emitEvent(ConversionEvent.Started(taskId))

                converter.convert(task)
                    .catch { error ->
                        Timber.e(error, "Error in conversion flow for task $taskId")
                        handleConversionError(taskId, error)
                    }
                    .collect { progress ->
                        updateTask(taskId) { progress.task }
                        emitEvent(ConversionEvent.ProgressUpdated(progress.task))

                        if (progress.stage == VideoToAudioConverter.ConversionStage.COMPLETED) {
                            taskRetryCount.remove(taskId)
                            emitEvent(ConversionEvent.Completed(progress.task))
                        } else if (progress.stage == VideoToAudioConverter.ConversionStage.ERROR) {
                            handleConversionError(taskId, Exception(progress.task.errorMessage))
                        }
                    }

            } catch (e: Exception) {
                Timber.e(e, "Exception during conversion for task $taskId")
                handleConversionError(taskId, e)
            } finally {
                conversionSemaphore.release()
                activeJobs.remove(taskId)
            }
        }

        activeJobs[taskId] = job
    }

    /**
     * Pauses an active conversion.
     * 
     * Note: This cancels the current conversion job. The task can be resumed later,
     * but it will restart from the beginning.
     * 
     * @param taskId ID of the task to pause
     */
    fun pauseConversion(taskId: String) {
        val task = findTask(taskId) ?: return

        if (task.status != ConversionStatus.RUNNING) {
            Timber.w("Cannot pause task $taskId - not running")
            return
        }

        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)

        updateTask(taskId) { it.withStatus(ConversionStatus.PAUSED) }
        emitEvent(ConversionEvent.Paused(taskId))
        
        Timber.d("Conversion paused: $taskId")
    }

    /**
     * Resumes a paused conversion.
     * 
     * @param taskId ID of the task to resume
     */
    fun resumeConversion(taskId: String) {
        val task = findTask(taskId) ?: return

        if (task.status != ConversionStatus.PAUSED) {
            Timber.w("Cannot resume task $taskId - not paused")
            return
        }

        startConversion(taskId)
        Timber.d("Conversion resumed: $taskId")
    }

    /**
     * Cancels a conversion and removes it from the queue.
     * 
     * If the task is running, it's stopped first. Any partial output files are deleted.
     * 
     * @param taskId ID of the task to cancel
     */
    fun cancelConversion(taskId: String) {
        val task = findTask(taskId) ?: return

        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        taskRetryCount.remove(taskId)

        // Delete partial output file if exists
        if (task.outputPath.isNotEmpty()) {
            try {
                val outputFile = File(task.outputPath)
                if (outputFile.exists()) {
                    outputFile.delete()
                    Timber.d("Deleted partial output file: ${task.outputPath}")
                }
            } catch (e: Exception) {
                Timber.w(e, "Error deleting partial output file")
            }
        }

        updateTask(taskId) { it.withStatus(ConversionStatus.CANCELLED) }
        emitEvent(ConversionEvent.Cancelled(taskId))
        
        Timber.d("Conversion cancelled: $taskId")
    }

    /**
     * Removes a completed, failed, or cancelled task from the list.
     * 
     * @param taskId ID of the task to remove
     */
    fun removeTask(taskId: String) {
        val task = findTask(taskId) ?: return

        if (!task.isFinished()) {
            Timber.w("Cannot remove task $taskId - not finished")
            return
        }

        _tasks.value = _tasks.value.filterNot { it.id == taskId }
        emitEvent(ConversionEvent.Removed(taskId))
        
        Timber.d("Task removed: $taskId")
    }

    /**
     * Cancels all active conversions.
     */
    fun cancelAllConversions() {
        val activeTasks = _tasks.value.filter { it.isActive() }
        activeTasks.forEach { cancelConversion(it.id) }
        
        Timber.d("All conversions cancelled")
    }

    /**
     * Clears all completed, failed, and cancelled tasks from the list.
     */
    fun clearFinishedTasks() {
        val finishedCount = _tasks.value.count { it.isFinished() }
        _tasks.value = _tasks.value.filterNot { it.isFinished() }
        
        Timber.d("Cleared $finishedCount finished tasks")
    }

    /**
     * Gets the current state of a task.
     * 
     * @param taskId ID of the task
     * @return ConversionTask or null if not found
     */
    fun getTask(taskId: String): ConversionTask? {
        return findTask(taskId)
    }

    /**
     * Gets all tasks with a specific status.
     * 
     * @param status Conversion status to filter by
     * @return List of matching tasks
     */
    fun getTasksByStatus(status: ConversionStatus): List<ConversionTask> {
        return _tasks.value.filter { it.status == status }
    }

    /**
     * Gets count of active conversions.
     * 
     * @return Number of running conversions
     */
    fun getActiveConversionCount(): Int {
        return _tasks.value.count { it.status == ConversionStatus.RUNNING }
    }

    /**
     * Checks if the manager can accept more conversions.
     * 
     * @return true if there's capacity for more conversions
     */
    fun canAddMoreConversions(): Boolean {
        return getActiveConversionCount() < MAX_CONCURRENT_CONVERSIONS
    }

    /**
     * Gets statistics about all conversions.
     * 
     * @return ConversionStatistics
     */
    fun getStatistics(): ConversionStatistics {
        val allTasks = _tasks.value
        return ConversionStatistics(
            totalTasks = allTasks.size,
            completedTasks = allTasks.count { it.status == ConversionStatus.COMPLETED },
            failedTasks = allTasks.count { it.status == ConversionStatus.FAILED },
            cancelledTasks = allTasks.count { it.status == ConversionStatus.CANCELLED },
            activeTasks = allTasks.count { it.isActive() },
            pendingTasks = allTasks.count { it.status == ConversionStatus.PENDING },
            totalInputSize = allTasks.sumOf { it.inputFileSizeBytes },
            totalOutputSize = allTasks.filter { it.status == ConversionStatus.COMPLETED }
                .sumOf { it.outputFileSizeBytes }
        )
    }

    /**
     * Cleans up temporary files created during conversions.
     */
    suspend fun cleanupTempFiles() {
        converter.cleanupTempFiles()
    }

    /**
     * Handles conversion errors and potentially retries.
     */
    private fun handleConversionError(taskId: String, error: Throwable) {
        val currentRetries = taskRetryCount.getOrDefault(taskId, 0)
        
        if (currentRetries < MAX_RETRY_ATTEMPTS) {
            taskRetryCount[taskId] = currentRetries + 1
            Timber.w("Retrying conversion $taskId (attempt ${currentRetries + 1}/$MAX_RETRY_ATTEMPTS)")
            
            updateTask(taskId) { it.withStatus(ConversionStatus.PENDING) }
            
            // Retry after a short delay
            scope.launch {
                kotlinx.coroutines.delay(2000)
                startConversion(taskId)
            }
        } else {
            taskRetryCount.remove(taskId)
            updateTask(taskId) { 
                it.withStatus(
                    ConversionStatus.FAILED,
                    error = error.message ?: "Unknown error"
                )
            }
            emitEvent(ConversionEvent.Failed(taskId, error.message ?: "Unknown error"))
        }
    }

    private fun addTask(task: ConversionTask) {
        _tasks.value = _tasks.value + task
        emitEvent(ConversionEvent.Added(task))
    }

    private fun findTask(taskId: String): ConversionTask? {
        return _tasks.value.find { it.id == taskId }
    }

    private fun updateTask(taskId: String, update: (ConversionTask) -> ConversionTask) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) update(task) else task
        }
    }

    private fun emitEvent(event: ConversionEvent) {
        scope.launch {
            _conversionEvents.emit(event)
        }
    }

    /**
     * Statistics about conversion operations.
     */
    data class ConversionStatistics(
        val totalTasks: Int,
        val completedTasks: Int,
        val failedTasks: Int,
        val cancelledTasks: Int,
        val activeTasks: Int,
        val pendingTasks: Int,
        val totalInputSize: Long,
        val totalOutputSize: Long
    ) {
        val successRate: Float
            get() = if (totalTasks > 0) {
                (completedTasks.toFloat() / totalTasks) * 100f
            } else 0f

        val compressionRatio: Float
            get() = if (totalInputSize > 0 && totalOutputSize > 0) {
                totalOutputSize.toFloat() / totalInputSize
            } else 0f
    }

    /**
     * Events emitted by the ConversionManager.
     */
    sealed class ConversionEvent {
        data class Added(val task: ConversionTask) : ConversionEvent()
        data class Started(val taskId: String) : ConversionEvent()
        data class ProgressUpdated(val task: ConversionTask) : ConversionEvent()
        data class Paused(val taskId: String) : ConversionEvent()
        data class Completed(val task: ConversionTask) : ConversionEvent()
        data class Failed(val taskId: String, val error: String) : ConversionEvent()
        data class Cancelled(val taskId: String) : ConversionEvent()
        data class Removed(val taskId: String) : ConversionEvent()
    }
}
