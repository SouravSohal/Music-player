package com.musicplayer.app.downloader

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a queue of download tasks with concurrency control.
 * 
 * This manager provides:
 * - Download queue management
 * - Concurrent download limits
 * - Pause/resume capability
 * - Cancel individual or all downloads
 * - Retry failed downloads
 * - Real-time status updates via Flow
 * - Persistent state across app restarts (when integrated with database)
 * 
 * The manager ensures that downloads are processed efficiently while
 * respecting system resources and network bandwidth.
 * 
 * @property mediaDownloader Core downloader implementation
 */
@Singleton
class DownloadManager @Inject constructor(
    private val mediaDownloader: MediaDownloader
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Active downloads with their jobs
    private val activeDownloads = ConcurrentHashMap<String, Job>()
    
    // All download tasks (active, queued, completed, failed)
    private val downloadTasks = ConcurrentHashMap<String, DownloadTask>()
    
    // Queue of pending downloads
    private val downloadQueue = ArrayDeque<String>()
    
    // Flow for broadcasting download state changes
    private val _downloadStates = MutableSharedFlow<DownloadTask>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val downloadStates: SharedFlow<DownloadTask> = _downloadStates.asSharedFlow()

    companion object {
        /**
         * Maximum number of concurrent downloads.
         * Can be adjusted based on network conditions and device capabilities.
         */
        private const val MAX_CONCURRENT_DOWNLOADS = 2
        
        /**
         * Maximum number of retry attempts for failed downloads.
         */
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    /**
     * Adds a new download to the queue.
     * 
     * @param task Download task to add
     * @return true if task was added, false if already exists
     */
    suspend fun addDownload(task: DownloadTask): Boolean {
        if (downloadTasks.containsKey(task.id)) {
            Timber.w("Download task already exists: ${task.id}")
            return false
        }

        downloadTasks[task.id] = task
        
        // Check if we can start immediately or need to queue
        if (activeDownloads.size < MAX_CONCURRENT_DOWNLOADS) {
            startDownload(task.id)
        } else {
            downloadQueue.add(task.id)
            val queuedTask = task.withStatus(DownloadStatus.QUEUED)
            downloadTasks[task.id] = queuedTask
            _downloadStates.emit(queuedTask)
        }

        return true
    }

    /**
     * Starts downloading a task by ID.
     * 
     * @param taskId ID of the task to start
     */
    private fun startDownload(taskId: String) {
        val task = downloadTasks[taskId] ?: return
        
        if (activeDownloads.containsKey(taskId)) {
            Timber.w("Download already active: $taskId")
            return
        }

        val job = scope.launch {
            try {
                mediaDownloader.downloadMedia(task).collect { updatedTask ->
                    downloadTasks[taskId] = updatedTask
                    _downloadStates.emit(updatedTask)

                    // Handle completion or failure
                    when (updatedTask.status) {
                        DownloadStatus.COMPLETED -> {
                            handleDownloadCompleted(taskId)
                        }
                        DownloadStatus.FAILED -> {
                            handleDownloadFailed(taskId, updatedTask)
                        }
                        DownloadStatus.CANCELLED -> {
                            handleDownloadCompleted(taskId)
                        }
                        else -> {
                            // Still in progress
                        }
                    }
                }
            } catch (e: CancellationException) {
                Timber.d("Download cancelled: $taskId")
            } catch (e: Exception) {
                Timber.e(e, "Download error: $taskId")
                val failedTask = task.withStatus(DownloadStatus.FAILED, e.message)
                downloadTasks[taskId] = failedTask
                _downloadStates.emit(failedTask)
                handleDownloadFailed(taskId, failedTask)
            }
        }

        activeDownloads[taskId] = job
    }

    /**
     * Handles successful download completion.
     * 
     * @param taskId ID of completed task
     */
    private suspend fun handleDownloadCompleted(taskId: String) {
        activeDownloads.remove(taskId)
        
        // Start next download from queue
        processQueue()
    }

    /**
     * Handles download failure.
     * 
     * @param taskId ID of failed task
     * @param task Failed task with error information
     */
    private suspend fun handleDownloadFailed(taskId: String, task: DownloadTask) {
        activeDownloads.remove(taskId)

        // Check if we should retry
        if (task.retryCount < MAX_RETRY_ATTEMPTS) {
            Timber.d("Retrying download (attempt ${task.retryCount + 1}): $taskId")
            delay(2000L * (task.retryCount + 1)) // Exponential backoff
            
            val retryTask = task.withRetry()
            downloadTasks[taskId] = retryTask
            _downloadStates.emit(retryTask)
            
            startDownload(taskId)
        } else {
            Timber.e("Download failed after ${task.retryCount} retries: $taskId")
            // Process next in queue
            processQueue()
        }
    }

    /**
     * Processes the next download from the queue.
     */
    private suspend fun processQueue() {
        if (downloadQueue.isNotEmpty() && activeDownloads.size < MAX_CONCURRENT_DOWNLOADS) {
            val nextTaskId = downloadQueue.removeFirst()
            val nextTask = downloadTasks[nextTaskId]
            
            if (nextTask != null && !nextTask.isFinished()) {
                startDownload(nextTaskId)
            } else {
                // Task was removed or finished, try next
                processQueue()
            }
        }
    }

    /**
     * Pauses a download.
     * 
     * @param taskId ID of the task to pause
     * @return true if task was paused
     */
    suspend fun pauseDownload(taskId: String): Boolean {
        val job = activeDownloads[taskId] ?: return false
        val task = downloadTasks[taskId] ?: return false

        if (task.status != DownloadStatus.DOWNLOADING) {
            return false
        }

        // Cancel the job
        job.cancel()
        activeDownloads.remove(taskId)

        // Update task status
        val pausedTask = task.withStatus(DownloadStatus.PAUSED)
        downloadTasks[taskId] = pausedTask
        _downloadStates.emit(pausedTask)

        // Process next in queue
        processQueue()

        return true
    }

    /**
     * Resumes a paused download.
     * 
     * @param taskId ID of the task to resume
     * @return true if task was resumed
     */
    suspend fun resumeDownload(taskId: String): Boolean {
        val task = downloadTasks[taskId] ?: return false

        if (task.status != DownloadStatus.PAUSED) {
            return false
        }

        // Check if we can start immediately or need to queue
        if (activeDownloads.size < MAX_CONCURRENT_DOWNLOADS) {
            startDownload(taskId)
        } else {
            downloadQueue.add(taskId)
            val queuedTask = task.withStatus(DownloadStatus.QUEUED)
            downloadTasks[taskId] = queuedTask
            _downloadStates.emit(queuedTask)
        }

        return true
    }

    /**
     * Cancels a download and removes it from the queue.
     * 
     * @param taskId ID of the task to cancel
     * @return true if task was cancelled
     */
    suspend fun cancelDownload(taskId: String): Boolean {
        val task = downloadTasks[taskId] ?: return false

        // Cancel active job if running
        val job = activeDownloads[taskId]
        if (job != null) {
            job.cancel()
            activeDownloads.remove(taskId)
        }

        // Remove from queue if queued
        downloadQueue.remove(taskId)

        // Update task status
        val cancelledTask = task.withStatus(DownloadStatus.CANCELLED)
        downloadTasks[taskId] = cancelledTask
        _downloadStates.emit(cancelledTask)

        // Process next in queue
        processQueue()

        return true
    }

    /**
     * Retries a failed download.
     * 
     * @param taskId ID of the task to retry
     * @return true if retry was initiated
     */
    suspend fun retryDownload(taskId: String): Boolean {
        val task = downloadTasks[taskId] ?: return false

        if (task.status != DownloadStatus.FAILED) {
            return false
        }

        // Reset task for retry
        val retryTask = task.withRetry()
        downloadTasks[taskId] = retryTask
        _downloadStates.emit(retryTask)

        // Start or queue the download
        if (activeDownloads.size < MAX_CONCURRENT_DOWNLOADS) {
            startDownload(taskId)
        } else {
            downloadQueue.add(taskId)
            val queuedTask = retryTask.withStatus(DownloadStatus.QUEUED)
            downloadTasks[taskId] = queuedTask
            _downloadStates.emit(queuedTask)
        }

        return true
    }

    /**
     * Cancels all active downloads and clears the queue.
     */
    suspend fun cancelAllDownloads() {
        // Cancel all active downloads
        activeDownloads.keys.toList().forEach { taskId ->
            cancelDownload(taskId)
        }

        // Clear queue
        downloadQueue.clear()
    }

    /**
     * Pauses all active downloads.
     */
    suspend fun pauseAllDownloads() {
        activeDownloads.keys.toList().forEach { taskId ->
            pauseDownload(taskId)
        }
    }

    /**
     * Resumes all paused downloads.
     */
    suspend fun resumeAllDownloads() {
        downloadTasks.values
            .filter { it.status == DownloadStatus.PAUSED }
            .forEach { task ->
                resumeDownload(task.id)
            }
    }

    /**
     * Gets a download task by ID.
     * 
     * @param taskId Task ID
     * @return Download task or null if not found
     */
    fun getDownload(taskId: String): DownloadTask? {
        return downloadTasks[taskId]
    }

    /**
     * Gets all download tasks.
     * 
     * @return List of all download tasks
     */
    fun getAllDownloads(): List<DownloadTask> {
        return downloadTasks.values.toList()
    }

    /**
     * Gets active downloads.
     * 
     * @return List of active download tasks
     */
    fun getActiveDownloads(): List<DownloadTask> {
        return downloadTasks.values.filter { it.isActive() }
    }

    /**
     * Gets completed downloads.
     * 
     * @return List of completed download tasks
     */
    fun getCompletedDownloads(): List<DownloadTask> {
        return downloadTasks.values.filter { it.status == DownloadStatus.COMPLETED }
    }

    /**
     * Gets failed downloads.
     * 
     * @return List of failed download tasks
     */
    fun getFailedDownloads(): List<DownloadTask> {
        return downloadTasks.values.filter { it.status == DownloadStatus.FAILED }
    }

    /**
     * Removes a completed or failed download from the list.
     * 
     * @param taskId Task ID to remove
     * @return true if task was removed
     */
    fun removeDownload(taskId: String): Boolean {
        val task = downloadTasks[taskId] ?: return false
        
        if (!task.isFinished()) {
            return false
        }

        downloadTasks.remove(taskId)
        return true
    }

    /**
     * Clears all completed downloads from the list.
     */
    fun clearCompletedDownloads() {
        downloadTasks.entries.removeIf { 
            it.value.status == DownloadStatus.COMPLETED 
        }
    }

    /**
     * Gets the current queue size.
     * 
     * @return Number of downloads in queue
     */
    fun getQueueSize(): Int {
        return downloadQueue.size
    }

    /**
     * Gets the number of active downloads.
     * 
     * @return Number of active downloads
     */
    fun getActiveCount(): Int {
        return activeDownloads.size
    }

    /**
     * Cleans up resources and cancels all downloads.
     */
    fun cleanup() {
        scope.cancel()
        activeDownloads.clear()
        downloadQueue.clear()
    }
}
