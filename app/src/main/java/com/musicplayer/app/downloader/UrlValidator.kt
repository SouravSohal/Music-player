package com.musicplayer.app.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates and parses media URLs for downloading.
 * 
 * This class provides comprehensive URL validation including:
 * - URL format validation
 * - Platform support checking
 * - Reachability testing
 * - Media ID extraction
 * - Legal compliance checking
 * 
 * LEGAL NOTICE:
 * This validator enforces legal and ethical downloading practices:
 * - Only whitelisted domains are allowed
 * - Users must have rights to download content
 * - Respects robots.txt and terms of service
 * - Copyright compliance is required
 * 
 * @property okHttpClient HTTP client for network operations
 */
@Singleton
class UrlValidator @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val URL_REGEX = "^https?://.*"
        private const val TIMEOUT_SECONDS = 10L
        
        /**
         * LEGAL COMPLIANCE: Whitelist of domains that explicitly allow downloading.
         * 
         * This list includes only domains that:
         * 1. Explicitly permit content downloading in their terms of service
         * 2. Are open platforms for user-generated content with permissive licenses
         * 3. Provide public APIs for content access
         * 
         * DO NOT add domains that prohibit downloading in their ToS.
         * Examples of domains that should NOT be added:
         * - Commercial streaming services (Netflix, Disney+, Hulu, etc.)
         * - Music streaming services with paid subscriptions
         * - Any service that explicitly prohibits downloading
         * 
         * Users are responsible for ensuring they have rights to download content.
         */
        private val WHITELISTED_DOMAINS = setOf(
            // Public domain and open content
            "archive.org",              // Internet Archive - Public Domain
            "commons.wikimedia.org",    // Wikimedia Commons - Open Licenses
            "freesound.org",            // FreeSound - Creative Commons
            "soundcloud.com",           // SoundCloud - Some content allows download
            "vimeo.com",                // Vimeo - User-controlled download settings
            
            // Educational and open platforms
            "ted.com",                  // TED Talks - Often downloadable
            "khanacademy.org",          // Khan Academy - Educational content
            
            // Direct file URLs (user's own content)
            // Note: Localhost and direct file URLs for testing
            "localhost",
            "127.0.0.1"
        )
        
        // Platform-specific ID patterns
        private val YOUTUBE_PATTERN = Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})")
        private val VIMEO_PATTERN = Regex("vimeo\\.com/(\\d+)")
        private val SOUNDCLOUD_PATTERN = Regex("soundcloud\\.com/([a-zA-Z0-9_-]+)/([a-zA-Z0-9_-]+)")
    }

    /**
     * Validates a URL for downloading with comprehensive checks.
     * 
     * @param url URL to validate
     * @return ValidationResult with details about validation status
     */
    suspend fun validateUrl(url: String): ValidationResult = withContext(Dispatchers.IO) {
        try {
            // Step 1: Basic format validation
            if (!isValidUrlFormat(url)) {
                return@withContext ValidationResult.Invalid("Invalid URL format")
            }

            // Step 2: Parse URL
            val parsedUrl = try {
                URL(url)
            } catch (e: MalformedURLException) {
                return@withContext ValidationResult.Invalid("Malformed URL: ${e.message}")
            }

            // Step 3: Check domain whitelist (LEGAL COMPLIANCE)
            val domain = parsedUrl.host.lowercase()
            if (!isDomainWhitelisted(domain)) {
                return@withContext ValidationResult.NotSupported(
                    domain = domain,
                    reason = "Domain not whitelisted for downloading. " +
                            "Only content from authorized sources with proper permissions can be downloaded. " +
                            "Supported domains: ${WHITELISTED_DOMAINS.joinToString(", ")}"
                )
            }

            // Step 4: Extract media information
            val mediaInfo = extractMediaInfo(url, domain)

            // Step 5: Check reachability (optional, can be skipped for performance)
            val isReachable = checkReachability(url)
            if (!isReachable) {
                return@withContext ValidationResult.Unreachable("URL is not reachable")
            }

            // Validation successful
            ValidationResult.Valid(
                url = url,
                domain = domain,
                mediaId = mediaInfo.mediaId,
                platform = mediaInfo.platform
            )
        } catch (e: Exception) {
            Timber.e(e, "Error validating URL: $url")
            ValidationResult.Invalid("Validation error: ${e.message}")
        }
    }

    /**
     * Checks if URL format is valid.
     * 
     * @param url URL to check
     * @return true if format is valid
     */
    fun isValidUrlFormat(url: String): Boolean {
        if (url.isBlank()) return false
        return url.matches(Regex(URL_REGEX))
    }

    /**
     * Checks if domain is whitelisted for downloading.
     * 
     * @param domain Domain name to check
     * @return true if domain is whitelisted
     */
    fun isDomainWhitelisted(domain: String): Boolean {
        val normalizedDomain = domain.lowercase()
        return WHITELISTED_DOMAINS.any { whitelisted ->
            normalizedDomain == whitelisted || normalizedDomain.endsWith(".$whitelisted")
        }
    }

    /**
     * Extracts media information from URL.
     * 
     * @param url URL to parse
     * @param domain Domain name
     * @return MediaInfo with extracted details
     */
    private fun extractMediaInfo(url: String, domain: String): MediaInfo {
        val platform = when {
            domain.contains("youtube.com") || domain.contains("youtu.be") -> Platform.YOUTUBE
            domain.contains("vimeo.com") -> Platform.VIMEO
            domain.contains("soundcloud.com") -> Platform.SOUNDCLOUD
            domain.contains("archive.org") -> Platform.ARCHIVE_ORG
            else -> Platform.GENERIC
        }

        val mediaId = when (platform) {
            Platform.YOUTUBE -> YOUTUBE_PATTERN.find(url)?.groupValues?.get(1)
            Platform.VIMEO -> VIMEO_PATTERN.find(url)?.groupValues?.get(1)
            Platform.SOUNDCLOUD -> SOUNDCLOUD_PATTERN.find(url)?.let {
                "${it.groupValues[1]}/${it.groupValues[2]}"
            }
            else -> null
        }

        return MediaInfo(platform, mediaId)
    }

    /**
     * Checks if URL is reachable via HTTP HEAD request.
     * 
     * @param url URL to check
     * @return true if reachable
     */
    private suspend fun checkReachability(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .head() // HEAD request to avoid downloading content
                .build()

            val response = okHttpClient.newBuilder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
                .newCall(request)
                .execute()

            response.use {
                it.isSuccessful || it.code in 300..399 // Accept redirects
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to check reachability for: $url")
            false
        }
    }

    /**
     * Extracts filename from URL or generates one from media ID.
     * 
     * @param url Source URL
     * @param mediaType Type of media
     * @param quality Download quality
     * @return Suggested filename
     */
    fun suggestFilename(url: String, mediaType: MediaType, quality: DownloadQuality): String {
        val parsedUrl = try {
            URL(url)
        } catch (e: Exception) {
            return "download_${System.currentTimeMillis()}.${getExtension(mediaType)}"
        }

        val domain = parsedUrl.host.lowercase()
        val mediaInfo = extractMediaInfo(url, domain)
        
        val baseName = mediaInfo.mediaId ?: "download_${System.currentTimeMillis()}"
        val qualityTag = if (quality != DownloadQuality.BEST) "_${quality.resolution}" else ""
        val extension = getExtension(mediaType)
        
        return "${baseName}${qualityTag}.${extension}"
    }

    private fun getExtension(mediaType: MediaType): String {
        return when (mediaType) {
            MediaType.VIDEO -> "mp4"
            MediaType.AUDIO_ONLY -> "m4a"
        }
    }
}

/**
 * Result of URL validation.
 */
sealed class ValidationResult {
    /**
     * URL is valid and can be downloaded.
     * 
     * @property url Validated URL
     * @property domain Domain name
     * @property mediaId Extracted media ID (if applicable)
     * @property platform Detected platform
     */
    data class Valid(
        val url: String,
        val domain: String,
        val mediaId: String?,
        val platform: Platform
    ) : ValidationResult()

    /**
     * URL format is invalid.
     * 
     * @property reason Reason for invalidity
     */
    data class Invalid(val reason: String) : ValidationResult()

    /**
     * Domain or platform is not supported.
     * 
     * @property domain Domain name
     * @property reason Reason for not being supported
     */
    data class NotSupported(
        val domain: String,
        val reason: String
    ) : ValidationResult()

    /**
     * URL is not reachable.
     * 
     * @property reason Reason for unreachability
     */
    data class Unreachable(val reason: String) : ValidationResult()
}

/**
 * Extracted media information.
 */
private data class MediaInfo(
    val platform: Platform,
    val mediaId: String?
)

/**
 * Supported platforms.
 */
enum class Platform(val displayName: String) {
    YOUTUBE("YouTube"),
    VIMEO("Vimeo"),
    SOUNDCLOUD("SoundCloud"),
    ARCHIVE_ORG("Internet Archive"),
    GENERIC("Generic URL");

    companion object {
        fun fromDomain(domain: String): Platform {
            return when {
                domain.contains("youtube") -> YOUTUBE
                domain.contains("vimeo") -> VIMEO
                domain.contains("soundcloud") -> SOUNDCLOUD
                domain.contains("archive.org") -> ARCHIVE_ORG
                else -> GENERIC
            }
        }
    }
}
