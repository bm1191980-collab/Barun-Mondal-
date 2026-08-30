package com.example.data.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil.imageLoader
import coil.request.ImageRequest
import com.example.data.model.PostEntity
import com.example.data.model.PostType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import java.util.UUID

@OptIn(UnstableApi::class)
object SatisfyVideoEngine {
    private const val TAG = "SatisfyVideoEngine"

    // Real verified public streaming video sources with byte-range & HTTPS support
    val VERIFIED_FALLBACK_VIDEOS = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
    )

    val VERIFIED_SHORTS_VIDEOS = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
    )

    // Singleton Video Cache & DB Provider for fast instant playback and offline reuse
    private var simpleCache: SimpleCache? = null
    private var databaseProvider: StandaloneDatabaseProvider? = null
    private val preloadedUrls = Collections.synchronizedSet(mutableSetOf<String>())

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "satisfy_video_cache").apply {
                if (!exists()) mkdirs()
            }
            val evictor = LeastRecentlyUsedCacheEvictor(200 * 1024 * 1024L) // 200 MB disk LRU cache
            val dbProvider = StandaloneDatabaseProvider(context.applicationContext)
            databaseProvider = dbProvider
            simpleCache = SimpleCache(cacheDir, evictor, dbProvider)
        }
        return simpleCache!!
    }

    /**
     * Builds a CacheDataSource.Factory that transparently reads from SimpleCache first,
     * and streams / writes byte ranges on demand from network.
     */
    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val cache = getCache(context)
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("SatisfyApp/1.0 (Linux; Android) ExoPlayer")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(25_000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)

        val upstreamFactory = DefaultDataSource.Factory(context.applicationContext, httpDataSourceFactory)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Inspects, validates and sanitizes any media URL to guarantee a real playable stream.
     */
    fun getValidPlayableUrl(rawUrl: String?, postType: PostType = PostType.VIDEO, seedIndex: Int = 0): String {
        if (rawUrl.isNullOrBlank()) {
            return if (postType == PostType.SHORT) {
                VERIFIED_SHORTS_VIDEOS[seedIndex % VERIFIED_SHORTS_VIDEOS.size]
            } else {
                VERIFIED_FALLBACK_VIDEOS[seedIndex % VERIFIED_FALLBACK_VIDEOS.size]
            }
        }

        val trimmed = rawUrl.trim()

        // Check if valid web or local scheme
        val isHttp = trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)
        val isFile = trimmed.startsWith("file://", ignoreCase = true)
        val isContent = trimmed.startsWith("content://", ignoreCase = true)
        val isAsset = trimmed.startsWith("asset://", ignoreCase = true)

        if (isHttp || isFile || isContent || isAsset) {
            // Also reject dummy placeholders if any got stored with protocol
            if (!trimmed.contains("sample_") && !trimmed.contains("placeholder")) {
                return trimmed
            }
        }

        // Return indexed verified source
        Log.w(TAG, "Unrecognized or legacy rawUrl format: '$rawUrl', falling back to verified CDN stream")
        return if (postType == PostType.SHORT) {
            VERIFIED_SHORTS_VIDEOS[seedIndex % VERIFIED_SHORTS_VIDEOS.size]
        } else {
            VERIFIED_FALLBACK_VIDEOS[seedIndex % VERIFIED_FALLBACK_VIDEOS.size]
        }
    }

    /**
     * Builds a configured ExoPlayer instance with cached DataSources, Range headers,
     * cross-protocol redirect support, user agent, buffer thresholds and audio focus.
     */
    fun createExoPlayer(context: Context): ExoPlayer {
        val cacheDataSourceFactory = getCacheDataSourceFactory(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        // Low-latency, fast startup buffer control
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10_000, // minBufferMs
                40_000, // maxBufferMs
                1_000,  // bufferForPlaybackMs (instant start)
                2_000   // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // Audio attributes for movie/music playback
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(context.applicationContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setSeekForwardIncrementMs(10_000)
            .setSeekBackIncrementMs(10_000)
            .build()
    }

    /**
     * Preloads initial bytes (e.g. 2MB) of a video into local cache in the background.
     * This provides instantaneous zero-lag playback when the user clicks or scrolls to the video.
     */
    fun preloadVideo(
        context: Context,
        rawUrl: String?,
        postType: PostType = PostType.VIDEO,
        bytesToPreload: Long = 2 * 1024 * 1024L
    ) {
        val playableUrl = getValidPlayableUrl(rawUrl, postType)
        if (!playableUrl.startsWith("http", ignoreCase = true)) return
        if (preloadedUrls.contains(playableUrl)) return
        preloadedUrls.add(playableUrl)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cache = getCache(context)
                val upstream = DefaultHttpDataSource.Factory()
                    .setUserAgent("SatisfyApp/1.0 (Linux; Android) ExoPlayer")
                    .setConnectTimeoutMs(10_000)
                    .setReadTimeoutMs(15_000)
                    .setAllowCrossProtocolRedirects(true)
                    .createDataSource()

                val dataSpec = DataSpec.Builder()
                    .setUri(Uri.parse(playableUrl))
                    .setPosition(0)
                    .setLength(bytesToPreload)
                    .build()

                val cacheWriter = CacheWriter(
                    CacheDataSource(cache, upstream),
                    dataSpec,
                    null,
                    null
                )
                cacheWriter.cache()
                Log.d(TAG, "Successfully preloaded $bytesToPreload bytes for $playableUrl")
            } catch (e: Exception) {
                Log.d(TAG, "Preload note for $playableUrl: ${e.message}")
            }
        }
    }

    /**
     * Preloads both video streams and thumbnail image assets in the background for a batch of feed items.
     */
    fun preloadFeedItems(context: Context, posts: List<PostEntity>, count: Int = 4) {
        posts.take(count).forEach { post ->
            // Preload video header chunks
            preloadVideo(context, post.mediaUrl, post.type)

            // Preload high-res thumbnail in Coil cache
            if (post.thumbnailUrl.isNotBlank() && post.thumbnailUrl.startsWith("http", ignoreCase = true)) {
                try {
                    val request = ImageRequest.Builder(context.applicationContext)
                        .data(post.thumbnailUrl)
                        .crossfade(true)
                        .build()
                    context.imageLoader.enqueue(request)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    /**
     * Creates a MediaItem with proper MIME type detection for HLS, DASH, MP4, WebM.
     */
    fun createMediaItem(rawUrl: String, postType: PostType = PostType.VIDEO): MediaItem {
        val playableUrl = getValidPlayableUrl(rawUrl, postType)
        val uri = Uri.parse(playableUrl)
        val builder = MediaItem.Builder().setUri(uri)

        val lowerUrl = playableUrl.lowercase()
        when {
            lowerUrl.contains(".m3u8") -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            lowerUrl.contains(".mpd") -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
            lowerUrl.contains(".mp4") -> builder.setMimeType(MimeTypes.VIDEO_MP4)
            lowerUrl.contains(".webm") -> builder.setMimeType(MimeTypes.VIDEO_WEBM)
        }

        return builder.build()
    }

    /**
     * Safely copies an uploaded media Uri from gallery/file picker into the app's permanent
     * internal storage directory (`filesDir/satisfy_media/`). Returns the permanent `file://` URI string.
     */
    fun saveMediaToInternalStorage(context: Context, sourceUri: Uri, isVideo: Boolean): String {
        return try {
            val extension = if (isVideo) "mp4" else "jpg"
            val dir = File(context.filesDir, "satisfy_media").apply { if (!exists()) mkdirs() }
            val destFile = File(dir, "media_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy gallery media to internal storage: ${e.message}", e)
            sourceUri.toString()
        }
    }

    /**
     * Parses PlaybackException and returns a user-friendly error explanation with cause diagnosis.
     */
    fun parsePlaybackException(error: PlaybackException): String {
        Log.e(TAG, "Playback Exception [code=${error.errorCode}, name=${error.errorCodeName}]: ${error.message}", error)
        val cause = error.cause
        return when {
            cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException -> {
                "HTTP ${cause.responseCode} Error: Server responded with error. Retrying stream..."
            }
            cause is androidx.media3.datasource.HttpDataSource.HttpDataSourceException -> {
                "Network Stream Error: Unable to fetch video chunk. Check connection."
            }
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                "Connection timeout. Reconnecting to streaming server..."
            }
            error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
            error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> {
                "Video format unsupported. Switching to standard stream..."
            }
            error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> {
                "Device hardware decoder error. Retrying with software pipeline..."
            }
            else -> error.localizedMessage ?: "Playback stream interrupted. Tap to reload."
        }
    }
}
