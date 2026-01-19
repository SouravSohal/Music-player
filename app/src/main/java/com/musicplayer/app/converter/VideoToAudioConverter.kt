package com.musicplayer.app.converter

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts video files to audio formats using MediaExtractor and MediaCodec.
 * 
 * This class provides comprehensive video-to-audio conversion with:
 * - Hardware-accelerated decoding and encoding via MediaCodec
 * - Support for multiple input video formats (MP4, MKV, AVI, MOV, etc.)
 * - Multiple output audio formats (MP3, AAC, FLAC, OGG)
 * - Real-time progress tracking via Flow
 * - Bitrate and quality selection
 * - Duration and file size estimation
 * - Proper resource management and cleanup
 * 
 * The conversion process:
 * 1. Extract audio track from video using MediaExtractor
 * 2. Decode audio to raw PCM using MediaCodec decoder
 * 3. Encode PCM to target format using MediaCodec encoder
 * 4. Write encoded data to output file using MediaMuxer
 * 
 * @property context Application context for file operations
 * @property audioEncoder Audio encoder for format conversion
 */
@Singleton
class VideoToAudioConverter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioEncoder: AudioEncoder
) {
    companion object {
        private const val TIMEOUT_US = 10000L
        private const val BUFFER_SIZE = 1024 * 1024 // 1MB buffer
        
        private const val OUTPUT_DIR_NAME = "ConvertedAudio"
        private const val TEMP_DIR_NAME = "ConversionTemp"
    }

    private val outputDir: File by lazy {
        File(context.getExternalFilesDir(null), OUTPUT_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    private val tempDir: File by lazy {
        File(context.cacheDir, TEMP_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Represents progress of an ongoing conversion.
     * 
     * @property task The conversion task being processed
     * @property stage Current conversion stage
     */
    data class ConversionProgress(
        val task: ConversionTask,
        val stage: ConversionStage
    )

    /**
     * Stages of the conversion process.
     */
    enum class ConversionStage {
        INITIALIZING,
        EXTRACTING,
        DECODING,
        ENCODING,
        FINALIZING,
        COMPLETED,
        ERROR
    }

    /**
     * Converts a video file to audio format.
     * 
     * Emits progress updates through a Flow, allowing real-time tracking of:
     * - Current conversion stage
     * - Progress percentage
     * - Estimated time remaining
     * - Output file size
     * 
     * @param task Conversion task configuration
     * @return Flow emitting ConversionProgress updates
     */
    fun convert(task: ConversionTask): Flow<ConversionProgress> = flow {
        var updatedTask = task.withStatus(ConversionStatus.RUNNING)
        emit(ConversionProgress(updatedTask, ConversionStage.INITIALIZING))

        try {
            // Validate input file
            val inputFileSize = getFileSize(task.sourceUri)
            updatedTask = updatedTask.copy(inputFileSizeBytes = inputFileSize)

            // Create output file
            val outputFile = File(outputDir, generateOutputFileName(task))
            updatedTask = updatedTask.copy(outputPath = outputFile.absolutePath)

            emit(ConversionProgress(updatedTask, ConversionStage.EXTRACTING))

            // Extract audio information from video
            val audioInfo = extractAudioInfo(task.sourceUri)
            if (audioInfo == null) {
                throw ConversionException("No audio track found in video file")
            }

            emit(ConversionProgress(
                updatedTask.withProgress(5f),
                ConversionStage.EXTRACTING
            ))

            // Estimate output size and duration
            val estimatedSize = updatedTask.estimateOutputSize(audioInfo.durationUs / 1000)
            updatedTask = updatedTask.copy(outputFileSizeBytes = estimatedSize)

            // Perform conversion
            val result = performConversion(
                sourceUri = task.sourceUri,
                outputFile = outputFile,
                audioInfo = audioInfo,
                targetFormat = task.outputFormat,
                targetBitrate = task.bitrate
            ) { progress, stage ->
                updatedTask = updatedTask.withProgress(
                    newProgress = 5f + (progress * 0.95f),
                    estimatedRemaining = calculateEstimatedTime(
                        progress,
                        updatedTask.getElapsedTimeMs() ?: 0L
                    ),
                    currentOutputSize = outputFile.length()
                )
                emit(ConversionProgress(updatedTask, stage))
            }

            if (result.success) {
                updatedTask = updatedTask
                    .withProgress(100f, currentOutputSize = outputFile.length())
                    .withStatus(ConversionStatus.COMPLETED)
                
                emit(ConversionProgress(updatedTask, ConversionStage.COMPLETED))
                
                Timber.d("Conversion completed: ${outputFile.absolutePath}")
            } else {
                throw ConversionException(result.errorMessage ?: "Conversion failed")
            }

        } catch (e: Exception) {
            Timber.e(e, "Error converting video to audio")
            updatedTask = updatedTask.withStatus(
                ConversionStatus.FAILED,
                error = e.message ?: "Unknown error"
            )
            emit(ConversionProgress(updatedTask, ConversionStage.ERROR))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Performs the actual conversion process.
     */
    private suspend fun performConversion(
        sourceUri: Uri,
        outputFile: File,
        audioInfo: AudioInfo,
        targetFormat: AudioFormat,
        targetBitrate: AudioBitrate,
        progressCallback: suspend (Float, ConversionStage) -> Unit
    ): ConversionResult {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        
        try {
            progressCallback(0f, ConversionStage.DECODING)

            // Setup extractor
            extractor = MediaExtractor().apply {
                setDataSource(context, sourceUri, null)
                selectTrack(audioInfo.trackIndex)
            }

            // Setup decoder
            decoder = MediaCodec.createDecoderByType(audioInfo.mimeType)
            decoder.configure(audioInfo.format, null, null, 0)
            decoder.start()

            progressCallback(5f, ConversionStage.DECODING)

            // Decode audio to PCM
            val pcmData = decodeAudioToPcm(
                extractor,
                decoder,
                audioInfo
            ) { progress ->
                progressCallback(5f + (progress * 0.4f), ConversionStage.DECODING)
            }

            progressCallback(45f, ConversionStage.ENCODING)

            // Create temporary encoded file
            val tempOutputFile = File(tempDir, "temp_${System.currentTimeMillis()}.${targetFormat.extension}")
            
            // Encode PCM to target format
            val encoderConfig = AudioEncoder.EncoderConfig(
                format = targetFormat,
                bitrate = targetBitrate,
                sampleRate = audioInfo.sampleRate,
                channelCount = audioInfo.channelCount
            )

            val encodingResult = audioEncoder.encode(
                inputData = pcmData,
                outputFile = tempOutputFile,
                config = encoderConfig
            ) { progress ->
                progressCallback(45f + (progress * 0.45f), ConversionStage.ENCODING)
            }

            if (!encodingResult.success) {
                return ConversionResult(
                    success = false,
                    errorMessage = encodingResult.errorMessage
                )
            }

            progressCallback(90f, ConversionStage.FINALIZING)

            // Mux the encoded audio into final container
            muxAudioToContainer(tempOutputFile, outputFile, targetFormat)

            // Clean up temp file
            tempOutputFile.delete()

            progressCallback(100f, ConversionStage.COMPLETED)

            return ConversionResult(
                success = true,
                outputFile = outputFile,
                bytesWritten = outputFile.length()
            )

        } catch (e: Exception) {
            Timber.e(e, "Error during conversion process")
            return ConversionResult(
                success = false,
                errorMessage = e.message ?: "Conversion error"
            )
        } finally {
            extractor?.release()
            decoder?.stop()
            decoder?.release()
            encoder?.stop()
            encoder?.release()
            muxer?.stop()
            muxer?.release()
        }
    }

    /**
     * Decodes audio from video to raw PCM format.
     */
    private fun decodeAudioToPcm(
        extractor: MediaExtractor,
        decoder: MediaCodec,
        audioInfo: AudioInfo,
        progressCallback: (Float) -> Unit
    ): ByteBuffer {
        val pcmBuffer = ByteBuffer.allocate(BUFFER_SIZE * 10) // Large buffer for PCM data
        val bufferInfo = MediaCodec.BufferInfo()
        var isInputDone = false
        var bytesDecoded = 0L
        val totalBytes = audioInfo.durationUs * audioInfo.sampleRate * audioInfo.channelCount * 2 / 1_000_000L

        while (true) {
            // Feed input to decoder
            if (!isInputDone) {
                val inputBufferId = decoder.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferId >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputBufferId)
                    inputBuffer?.let { buffer ->
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputBufferId,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isInputDone = true
                        } else {
                            val presentationTime = extractor.sampleTime
                            decoder.queueInputBuffer(
                                inputBufferId,
                                0,
                                sampleSize,
                                presentationTime,
                                0
                            )
                            extractor.advance()
                        }
                    }
                }
            }

            // Get decoded output
            val outputBufferId = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            when {
                outputBufferId >= 0 -> {
                    val outputBuffer = decoder.getOutputBuffer(outputBufferId)
                    outputBuffer?.let { buffer ->
                        if (bufferInfo.size > 0 && pcmBuffer.remaining() >= bufferInfo.size) {
                            val data = ByteArray(bufferInfo.size)
                            buffer.get(data)
                            pcmBuffer.put(data)
                            bytesDecoded += bufferInfo.size.toLong()
                            
                            progressCallback((bytesDecoded.toFloat() / totalBytes) * 100f)
                        }
                    }

                    decoder.releaseOutputBuffer(outputBufferId, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
                outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    Timber.d("Decoder output format changed: ${decoder.outputFormat}")
                }
            }
        }

        pcmBuffer.flip()
        return pcmBuffer
    }

    /**
     * Muxes encoded audio into final container format.
     */
    private fun muxAudioToContainer(
        inputFile: File,
        outputFile: File,
        format: AudioFormat
    ) {
        // For most formats, we can just copy the file
        // MediaMuxer is primarily for MP4/M4A containers
        when (format) {
            AudioFormat.M4A, AudioFormat.AAC -> {
                // Use MediaMuxer for proper MP4 container
                val extractor = MediaExtractor().apply {
                    setDataSource(inputFile.absolutePath)
                }
                
                val muxer = MediaMuxer(
                    outputFile.absolutePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )
                
                val trackFormat = extractor.getTrackFormat(0)
                val trackIndex = muxer.addTrack(trackFormat)
                muxer.start()
                
                extractor.selectTrack(0)
                val buffer = ByteBuffer.allocate(BUFFER_SIZE)
                val bufferInfo = MediaCodec.BufferInfo()
                
                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags
                    
                    muxer.writeSampleData(trackIndex, buffer, bufferInfo)
                    extractor.advance()
                }
                
                muxer.stop()
                muxer.release()
                extractor.release()
            }
            else -> {
                // For other formats, direct copy is sufficient
                inputFile.copyTo(outputFile, overwrite = true)
            }
        }
    }

    /**
     * Extracts audio track information from video file.
     */
    private fun extractAudioInfo(uri: Uri): AudioInfo? {
        var extractor: MediaExtractor? = null
        
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mimeType = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mimeType.startsWith("audio/")) {
                    val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    val durationUs = format.getLong(MediaFormat.KEY_DURATION)
                    val bitrate = try {
                        format.getInteger(MediaFormat.KEY_BIT_RATE)
                    } catch (e: Exception) {
                        128000 // Default
                    }

                    return AudioInfo(
                        trackIndex = i,
                        mimeType = mimeType,
                        format = format,
                        sampleRate = sampleRate,
                        channelCount = channelCount,
                        durationUs = durationUs,
                        bitrate = bitrate
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error extracting audio info from URI: $uri")
        } finally {
            extractor?.release()
        }

        return null
    }

    /**
     * Gets the file size of a URI.
     */
    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (e: Exception) {
            Timber.w(e, "Unable to get file size for URI: $uri")
            0L
        }
    }

    /**
     * Generates output filename based on task configuration.
     */
    private fun generateOutputFileName(task: ConversionTask): String {
        val baseName = File(task.sourcePath).nameWithoutExtension
        val timestamp = System.currentTimeMillis()
        val bitrate = task.bitrate.value
        return "${baseName}_${bitrate}kbps_$timestamp.${task.outputFormat.extension}"
    }

    /**
     * Calculates estimated time remaining.
     */
    private fun calculateEstimatedTime(progress: Float, elapsedMs: Long): Long? {
        if (progress <= 0f) return null
        val totalEstimatedMs = (elapsedMs / progress) * 100f
        return (totalEstimatedMs - elapsedMs).toLong()
    }

    /**
     * Gets the output directory where converted files are stored.
     */
    fun getOutputDirectory(): File = outputDir

    /**
     * Cleans up temporary files from the temp directory.
     */
    suspend fun cleanupTempFiles() {
        try {
            tempDir.listFiles()?.forEach { file ->
                file.delete()
            }
            Timber.d("Temporary files cleaned up")
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up temporary files")
        }
    }

    /**
     * Validates that a video file contains an audio track.
     */
    suspend fun validateVideoFile(uri: Uri): Boolean {
        return extractAudioInfo(uri) != null
    }

    /**
     * Information about an audio track in a video file.
     */
    private data class AudioInfo(
        val trackIndex: Int,
        val mimeType: String,
        val format: MediaFormat,
        val sampleRate: Int,
        val channelCount: Int,
        val durationUs: Long,
        val bitrate: Int
    )

    /**
     * Result of a conversion operation.
     */
    private data class ConversionResult(
        val success: Boolean,
        val outputFile: File? = null,
        val errorMessage: String? = null,
        val bytesWritten: Long = 0L
    )

    /**
     * Exception thrown during conversion.
     */
    class ConversionException(message: String) : Exception(message)
}
