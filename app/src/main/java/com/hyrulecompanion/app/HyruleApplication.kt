package com.hyrulecompanion.app

import android.app.Application
import android.util.Base64
import android.util.LruCache
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.memory.MemoryCache
import coil.request.Options
import com.hyrulecompanion.app.network.CommunityMediaCache
import com.hyrulecompanion.app.data.JOURNEY_PREFERENCES
import com.hyrulecompanion.app.ui.LANGUAGE_KEY
import com.hyrulecompanion.app.ui.initTranslations
import okio.Buffer

class HyruleApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        initTranslations(this, getSharedPreferences(JOURNEY_PREFERENCES, MODE_PRIVATE).getString(LANGUAGE_KEY, null))
    }

    internal val communityMediaCache by lazy { CommunityMediaCache(this) }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).build() }
        .diskCache {
            DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(64L * 1024 * 1024).build()
        }
        .components { add(ProfileImageFetcher()) }
        .build()
}

internal class ProfileImageData(val encoded: String, val digest: String)

private class ProfileImageFetcher : Fetcher.Factory<ProfileImageData> {
    private val bytes = object : LruCache<String, ByteArray>(5 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    override fun create(data: ProfileImageData, options: Options, imageLoader: ImageLoader) = Fetcher {
        val decoded = bytes.get(data.digest) ?: Base64.decode(
            data.encoded.substringAfter("base64,", ""), Base64.DEFAULT
        ).takeIf { it.isNotEmpty() }?.also { bytes.put(data.digest, it) }
            ?: return@Fetcher null
        SourceResult(
            source = ImageSource(Buffer().write(decoded), options.context),
            mimeType = data.encoded.substringAfter("data:").substringBefore(';'),
            dataSource = DataSource.MEMORY
        )
    }
}
