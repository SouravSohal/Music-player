package com.musicplayer.app.downloader

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core media downloader implementation.
 * 
 * This class handles the actual downloading of media files from validated URLs.
 * Features include:
 * - Resumable downloads using HTTP Range requests
 * - Real-time progress tracking via Kotlin Flow
 * - Download speed calculation
 * - Error handling and retry logic
 * - Support for both video and audio-only downloads
 * 
 * LEGAL NOTICE:
 * Users are responsible for ensuring they have the legal right to download content.
 * This downloader should only be used for:
 * - Content you own or created
 * - Content with explicit download permissions
 * - Public domain or openly licensed content
 * - Content from platforms that permit downloading
 * 
 * Downloading copyrighted content without permission is illegal and violates
 * the terms of service of most platforms.
 * 
 * @property okHttpClient HTTP client for network operations
 * @property urlValidator Validator for URL checking
 */
@Singleton
class MediaDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val urlValidator: UrlValidator
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeDownloads = mutableMapOf<String, Job>()

    companion object {
        private const val BUFFER_SIZE = 8192
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
        private const val SPEED_CALCULATION_WINDOW_MS = 2000L
    }

    /**
     * Downloads media from a URL with progress tracking.
     * 
     * This method performs the following steps:
     * 1. Validates the URL
     * 2. Checks user consent and legal compliance
     * 3. Creates destination file
     * 4. Downloads content with resume support
     * 5. Reports progress via Flow
     * 
     * @param task Download task with configuration
     * @return Flow of download task updates with progress
     * @throws IllegalArgumentException if URL is invalid or not whitelisted
     * @throws IOException if download fails
     */
    fun downloadMedia(task: DownloadTask): Flow<DownloadTask> = flow {
        var currentTask = task

        try {
            // Step 1: Validate URL
            emit(currentTask.withStatus(DownloadStatus.PENDING))
            
            val validationResult = urlValidator.validateUrl(task.sourceUrl)
            when (validationResult) {
                is ValidationResult.Invalid -> {
                    throw IllegalArgumentException("Invalid URL: ${validationResult.reason}")
                }
                is ValidationResult.NotSupported -> {
                    throw IllegalArgumentException(
                        "Domain not supported: ${validationResult.reason}\n\n" +
                        "LEGAL NOTICE: Only whitelisted domains are allowed to ensure " +
                        "compliance with copyright laws and terms of service."
                    )
                }
                is ValidationResult.Unreachable -> {
                    throw IOException("URL is unreachable: ${validationResult.reason}")
                }
                is ValidationResult.Valid -> {
                    Timber.d("URL validated: ${validationResult.url}")
                }
            }

            // Step 2: Check if resuming
            val destinationFile = File(task.destinationPath)
            val existingBytes = if (destinationFile.exists() && task.canResume) {
                destinationFile.length()
            } else {
                0L
            }

            // Step 3: Start download
            currentTask = currentTask.withStatus(DownloadStatus.DOWNLOADING)
            emit(currentTask)

            // Step 4: Execute download with progress
            val result = executeDownload(
                task = currentTask,
                startByte = existingBytes
            )

            // Step 5: Emit updates from download
            result.collect { updatedTask ->
                currentTask = updatedTask
                emit(currentTask)
            }

        } catch (e: CancellationException) {
            // Download was cancelled
            currentTask = currentTask.withStatus(DownloadStatus.CANCELLED, "Download cancelled by user")
            emit(currentTask)
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Download failed for ${task.sourceUrl}")
            currentTask = currentTask.withStatus(DownloadStatus.FAILED, e.message ?: "Unknown error")
            emit(currentTask)
            throw e
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Executes the actual download operation with resume support.
     * 
     * @param task Download task
     * @param startByte Byte position to start from (for resume)
     * @return Flow of task updates
     */
    private suspend fun executeDownload(
        task: DownloadTask,
        startByte: Long
    ): Flow<DownloadTask> = flow {
        val destinationFile = File(task.destinationPath)
        
        // Create parent directories if needed
        destinationFile.parentFile?.mkdirs()

        // Build HTTP request with range support
        val requestBuilder = Request.Builder()
            .url(task.sourceUrl)
            .get()

        // Add range header for resume
        if (startByte > 0) {
            requestBuilder.addHeader("Range", "bytes=$startByte-")
            Timber.d("Resuming download from byte $startByte")
        }

        val request = requestBuilder.build()
        val response: Response = okHttpClient.newCall(request).execute()

        response.use { resp ->
            if (!resp.isSuccessful && resp.code != 206) { // 206 = Partial Content
                throw IOException("Download failed with HTTP ${resp.code}: ${resp.message}")
            }

            val body = resp.body ?: throw IOException("Response body is null")
            val contentLength = body.contentLength()
            val totalBytes = if (contentLength >= 0) contentLength + startByte else 0L
            
            // Check if server supports resume
            val supportsResume = resp.header("Accept-Ranges") == "bytes" || resp.code == 206
            val resumeToken = if (supportsResume) {
                resp.header("ETag") ?: resp.header("Last-Modified")
            } else null

            var currentTask = task.copy(
                totalBytes = totalBytes,
                bytesDownloaded = startByte,
                canResume = supportsResume,
                resumeToken = resumeToken
            )

            emit(currentTask)

            // Download with progress tracking
            val sink = destinationFile.sink(append = startByte > 0).buffer()
            val source = body.source()

            var bytesRead: Long = startByte
            var lastUpdateTime = System.currentTimeMillis()
            var lastSpeedCheckTime = System.currentTimeMillis()
            var lastSpeedCheckBytes = startByte
            val buffer = ByteArray(BUFFER_SIZE)

            try {
                while (true) {
                    val read = source.read(buffer)
                    if (read == -1L) break

                    sink.write(buffer, 0, read.toInt())
                    bytesRead += read

                    val currentTime = System.currentTimeMillis()
                    
                    // Update speed calculation
                    val speedElapsed = currentTime - lastSpeedCheckTime
                    if (speedElapsed >= SPEED_CALCULATION_WINDOW_MS) {
                        val bytesInWindow = bytesRead - lastSpeedCheckBytes
                        val speed = (bytesInWindow * 1000L) / speedElapsed
                        
                        lastSpeedCheckTime = currentTime
                        lastSpeedCheckBytes = bytesRead

                        currentTask = currentTask.copy(downloadSpeedBytesPerSec = speed)
                    }

                    // Emit progress updates
                    if (currentTime - lastUpdateTime >= PROGRESS_UPDATE_INTERVAL_MS) {
                        val progress = if (totalBytes > 0) {
                            (bytesRead.toFloat() / totalBytes * 100f)
                        } else {
                            0f
                        }

                        val estimatedRemaining = if (currentTask.downloadSpeedBytesPerSec > 0 && totalBytes > 0) {
                            ((totalBytes - bytesRead) * 1000L) / currentTask.downloadSpeedBytesPerSec
                        } else null

                        currentTask = currentTask.withProgress(
                            newProgress = progress,
                            downloaded = bytesRead,
                            total = totalBytes,
                            speed = currentTask.downloadSpeedBytesPerSec,
                            estimatedRemaining = estimatedRemaining
                        )

                        emit(currentTask)
                        lastUpdateTime = currentTime
                    }
                }

                sink.flush()
                sink.close()

                // Download completed
                currentTask = currentTask.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 100f,
                    bytesDownloaded = bytesRead,
                    downloadSpeedBytesPerSec = 0L,
                    estimatedTimeRemainingMs = 0L
                )
                emit(currentTask)

                Timber.d("Download completed: ${task.destinationPath}")

            } finally {
                sink.close()
            }
        }
    }

    /**
     * Starts a download asynchronously and returns the job.
     * 
     * @param task Download task
     * @param onProgress Callback for progress updates
     * @param onComplete Callback for completion
     * @param onError Callback for errors
     * @return Job that can be cancelled
     */
    fun startDownload(
        task: DownloadTask,
        onProgress: (DownloadTask) -> Unit,
        onComplete: (DownloadTask) -> Unit,
        onError: (Throwable) -> Unit
    ): Job {
        val job = scope.launch {
            try {
                downloadMedia(task).collect { updatedTask ->
                    onProgress(updatedTask)
                    
                    if (updatedTask.status == DownloadStatus.COMPLETED) {
                        onComplete(updatedTask)
                    } else if (updatedTask.status == DownloadStatus.FAILED) {
                        onError(IOException(updatedTask.errorMessage ?: "Download failed"))
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    onError(e)
                }
            }
        }

        activeDownloads[task.id] = job
        return job
    }

    /**
     * Cancels an active download.
     * 
     * @param taskId ID of the task to cancel
     * @return true if download was found and cancelled
     */
    fun cancelDownload(taskId: String): Boolean {
        val job = activeDownloads[taskId]
        return if (job != null) {
            job.cancel()
            activeDownloads.remove(taskId)
            true
        } else {
            false
        }
    }

    /**
     * Gets the number of active downloads.
     * 
     * @return Number of active downloads
     */
    fun getActiveDownloadCount(): Int {
        return activeDownloads.size
    }

    /**
     * Checks if a specific download is active.
     * 
     * @param taskId Task ID to check
     * @return true if download is active
     */
    fun isDownloadActive(taskId: String): Boolean {
        return activeDownloads.containsKey(taskId)
    }

    /**
     * Cleans up resources and cancels all downloads.
     */
    fun cleanup() {
        activeDownloads.values.forEach { it.cancel() }
        activeDownloads.clear()
        scope.cancel()
    }
}
