package com.musicplayer.app.converter

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles audio encoding to various formats using MediaCodec.
 * 
 * This class provides hardware-accelerated audio encoding with support for:
 * - MP3 encoding (via AAC as fallback)
 * - AAC encoding
 * - FLAC encoding (lossless)
 * - Multiple bitrates and sample rates
 * - Real-time progress callbacks
 * 
 * Note: Android's MediaCodec API has limited native support for MP3 encoding.
 * For MP3 output, this encoder uses AAC and relies on container format conversion.
 * 
 * @constructor Creates an AudioEncoder instance
 */
@Singleton
class AudioEncoder @Inject constructor() {

    companion object {
        private const val TIMEOUT_US = 10000L
        private const val DEFAULT_SAMPLE_RATE = 44100
        private const val DEFAULT_CHANNEL_COUNT = 2
        
        // AAC specific constants
        private const val AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC
        
        // FLAC specific constants
        private const val FLAC_COMPRESSION_LEVEL = 5
    }

    /**
     * Configuration for audio encoding.
     * 
     * @property format Output audio format
     * @property bitrate Target bitrate in kbps
     * @property sampleRate Sample rate in Hz
     * @property channelCount Number of audio channels (1 = mono, 2 = stereo)
     */
    data class EncoderConfig(
        val format: AudioFormat,
        val bitrate: AudioBitrate,
        val sampleRate: Int = DEFAULT_SAMPLE_RATE,
        val channelCount: Int = DEFAULT_CHANNEL_COUNT
    )

    /**
     * Result of an encoding operation.
     * 
     * @property success Whether encoding was successful
     * @property outputFile Output file if successful
     * @property errorMessage Error message if failed
     * @property bytesWritten Number of bytes written to output file
     */
    data class EncodingResult(
        val success: Boolean,
        val outputFile: File? = null,
        val errorMessage: String? = null,
        val bytesWritten: Long = 0L
    )

    /**
     * Encodes audio data to the specified format.
     * 
     * This method takes raw PCM audio data and encodes it using MediaCodec.
     * Progress is reported through the callback as a percentage (0-100).
     * 
     * @param inputData Raw PCM audio data
     * @param outputFile Target output file
     * @param config Encoder configuration
     * @param progressCallback Callback for progress updates (0-100)
     * @return EncodingResult indicating success or failure
     */
    suspend fun encode(
        inputData: ByteBuffer,
        outputFile: File,
        config: EncoderConfig,
        progressCallback: ((Float) -> Unit)? = null
    ): EncodingResult {
        var encoder: MediaCodec? = null
        var outputStream: FileOutputStream? = null

        try {
            val mediaFormat = createMediaFormat(config)
            val mimeType = getMimeTypeForFormat(config.format)

            encoder = MediaCodec.createEncoderByType(mimeType)
            encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            outputStream = FileOutputStream(outputFile)
            val totalInputSize = inputData.remaining()
            var bytesProcessed = 0L
            var bytesWritten = 0L

            val bufferInfo = MediaCodec.BufferInfo()
            var isInputDone = false

            while (true) {
                // Feed input data
                if (!isInputDone) {
                    val inputBufferId = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferId >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferId)
                        inputBuffer?.let { buffer ->
                            buffer.clear()
                            val chunkSize = minOf(buffer.remaining(), inputData.remaining())
                            
                            if (chunkSize > 0) {
                                val tempArray = ByteArray(chunkSize)
                                inputData.get(tempArray)
                                buffer.put(tempArray)
                                bytesProcessed += chunkSize.toLong()
                                
                                encoder.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    chunkSize,
                                    0,
                                    0
                                )

                                progressCallback?.invoke((bytesProcessed.toFloat() / totalInputSize) * 50f)
                            } else {
                                encoder.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                isInputDone = true
                            }
                        }
                    }
                }

                // Get output data
                val outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputBufferId >= 0 -> {
                        val outputBuffer = encoder.getOutputBuffer(outputBufferId)
                        outputBuffer?.let { buffer ->
                            if (bufferInfo.size > 0) {
                                val data = ByteArray(bufferInfo.size)
                                buffer.get(data)
                                outputStream.write(data)
                                bytesWritten += data.size.toLong()
                            }
                        }

                        encoder.releaseOutputBuffer(outputBufferId, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            progressCallback?.invoke(100f)
                            break
                        }

                        val encodingProgress = 50f + ((bytesWritten.toFloat() / 
                            estimateOutputSize(totalInputSize.toLong(), config)) * 50f)
                        progressCallback?.invoke(encodingProgress.coerceAtMost(99f))
                    }
                    outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Timber.d("Output format changed: ${encoder.outputFormat}")
                    }
                    outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // No output available yet
                    }
                }
            }

            Timber.d("Encoding completed: ${outputFile.name}, bytes written: $bytesWritten")
            return EncodingResult(
                success = true,
                outputFile = outputFile,
                bytesWritten = bytesWritten
            )

        } catch (e: Exception) {
            Timber.e(e, "Error encoding audio to ${config.format}")
            
            if (outputFile.exists()) {
                outputFile.delete()
            }
            
            return EncodingResult(
                success = false,
                errorMessage = e.message ?: "Unknown encoding error"
            )
        } finally {
            try {
                outputStream?.close()
                encoder?.stop()
                encoder?.release()
            } catch (e: Exception) {
                Timber.w(e, "Error releasing encoder resources")
            }
        }
    }

    /**
     * Creates a MediaFormat for the specified encoder configuration.
     * 
     * @param config Encoder configuration
     * @return Configured MediaFormat
     */
    private fun createMediaFormat(config: EncoderConfig): MediaFormat {
        val mimeType = getMimeTypeForFormat(config.format)
        val mediaFormat = MediaFormat.createAudioFormat(
            mimeType,
            config.sampleRate,
            config.channelCount
        )

        when (config.format) {
            AudioFormat.AAC, AudioFormat.M4A, AudioFormat.MP3 -> {
                mediaFormat.setInteger(
                    MediaFormat.KEY_BIT_RATE,
                    config.bitrate.value * 1000
                )
                mediaFormat.setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    AAC_PROFILE
                )
            }
            AudioFormat.FLAC -> {
                mediaFormat.setInteger(
                    MediaFormat.KEY_FLAC_COMPRESSION_LEVEL,
                    FLAC_COMPRESSION_LEVEL
                )
            }
            AudioFormat.OGG -> {
                mediaFormat.setInteger(
                    MediaFormat.KEY_BIT_RATE,
                    config.bitrate.value * 1000
                )
            }
        }

        return mediaFormat
    }

    /**
     * Gets the MIME type for encoding based on the output format.
     * 
     * For MP3, we use AAC encoding as MediaCodec doesn't natively support MP3 encoding.
     * The actual MP3 container wrapping happens in the converter.
     * 
     * @param format Target audio format
     * @return MIME type for MediaCodec
     */
    private fun getMimeTypeForFormat(format: AudioFormat): String {
        return when (format) {
            AudioFormat.MP3 -> MediaFormat.MIMETYPE_AUDIO_AAC // Use AAC for MP3
            AudioFormat.AAC, AudioFormat.M4A -> MediaFormat.MIMETYPE_AUDIO_AAC
            AudioFormat.FLAC -> MediaFormat.MIMETYPE_AUDIO_FLAC
            AudioFormat.OGG -> MediaFormat.MIMETYPE_AUDIO_VORBIS
        }
    }

    /**
     * Estimates the output file size based on bitrate.
     * 
     * @param inputSize Input data size in bytes
     * @param config Encoder configuration
     * @return Estimated output size in bytes
     */
    private fun estimateOutputSize(inputSize: Long, config: EncoderConfig): Long {
        val inputDurationSeconds = inputSize / (config.sampleRate * config.channelCount * 2) // 16-bit samples
        val bitrateInBytesPerSecond = (config.bitrate.value * 1000) / 8
        return inputDurationSeconds * bitrateInBytesPerSecond
    }

    /**
     * Checks if the device supports encoding to the specified format.
     * 
     * @param format Audio format to check
     * @return true if supported, false otherwise
     */
    fun isFormatSupported(format: AudioFormat): Boolean {
        return try {
            val mimeType = getMimeTypeForFormat(format)
            val codecList = MediaCodec.createEncoderByType(mimeType)
            codecList.release()
            true
        } catch (e: Exception) {
            Timber.w(e, "Format ${format.displayName} not supported for encoding")
            false
        }
    }

    /**
     * Gets a list of all supported audio formats on this device.
     * 
     * @return List of supported AudioFormat values
     */
    fun getSupportedFormats(): List<AudioFormat> {
        return AudioFormat.values().filter { isFormatSupported(it) }
    }

    /**
     * Validates encoder configuration.
     * 
     * @param config Encoder configuration to validate
     * @return true if valid, false otherwise
     */
    fun validateConfig(config: EncoderConfig): Boolean {
        return when {
            config.sampleRate < 8000 || config.sampleRate > 192000 -> {
                Timber.w("Invalid sample rate: ${config.sampleRate}")
                false
            }
            config.channelCount < 1 || config.channelCount > 8 -> {
                Timber.w("Invalid channel count: ${config.channelCount}")
                false
            }
            !isFormatSupported(config.format) -> {
                Timber.w("Format not supported: ${config.format}")
                false
            }
            else -> true
        }
    }

    /**
     * Creates a raw PCM MediaFormat for decoding.
     * 
     * @param sampleRate Sample rate in Hz
     * @param channelCount Number of channels
     * @return PCM MediaFormat
     */
    fun createPcmFormat(
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        channelCount: Int = DEFAULT_CHANNEL_COUNT
    ): MediaFormat {
        return MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_RAW,
            sampleRate,
            channelCount
        ).apply {
            setInteger(MediaFormat.KEY_PCM_ENCODING, MediaCodecInfo.CodecCapabilities.ENCODING_PCM_16BIT)
        }
    }
}
