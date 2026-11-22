package com.hyrulecompanion.app.network

import android.content.Context
import android.util.LruCache
import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

internal data class CachedMedia(val revision: String, val images: CommunityMedia)

@OptIn(ExperimentalCoilApi::class)
internal class CommunityMediaCache(context: Context) {
    private val memory = object : LruCache<String, CachedMedia>(10 * 1024 * 1024) {
        override fun sizeOf(key: String, value: CachedMedia): Int =
            128 + 2 * (value.images.avatar.orEmpty().length + value.images.banner.orEmpty().length)
    }
    private val disk by lazy {
        DiskCache.Builder().directory(context.cacheDir.resolve("community-media"))
            .maxSizeBytes(24L * 1024 * 1024).build()
    }

    // Entries are only displayed after the server confirms the revision under the viewer's RLS.
    @Synchronized fun get(key: String): CachedMedia? {
        memory.get(key)?.let { return it }
        try {
            return disk.openSnapshot(key)?.use { snapshot ->
                val revision = disk.fileSystem.read(snapshot.metadata) { readUtf8() }
                val row = JSONObject(disk.fileSystem.read(snapshot.data) { readUtf8() })
                CachedMedia(revision, readCommunityMedia(row)).also { memory.put(key, it) }
            }
        } catch (_: IOException) {
            remove(key)
        } catch (_: JSONException) {
            remove(key)
        }
        return null
    }

    @Synchronized fun put(key: String, value: CachedMedia) {
        memory.put(key, value)
        quietly {
            val editor = disk.openEditor(key) ?: return@quietly
            try {
                disk.fileSystem.write(editor.metadata) { writeUtf8(value.revision) }
                disk.fileSystem.write(editor.data) {
                    writeUtf8(JSONObject()
                        .put("avatar_image", value.images.avatar ?: JSONObject.NULL)
                        .put("banner_image", value.images.banner ?: JSONObject.NULL).toString())
                }
                editor.commit()
            } catch (failure: IOException) {
                editor.abort()
                throw failure
            }
        }
    }

    @Synchronized fun remove(key: String) {
        memory.remove(key)
        quietly { disk.remove(key) }
    }

    @Synchronized fun clear() {
        memory.evictAll()
        quietly { disk.clear() }
    }

    private inline fun quietly(action: () -> Unit) {
        try { action() } catch (_: IOException) {
            // The server request can still use memory when Android's disk cache is unavailable.
        }
    }
}

internal fun readCommunityMedia(row: JSONObject) = CommunityMedia(
    row.optString("avatar_image").takeUnless { row.isNull("avatar_image") },
    row.optString("banner_image").takeUnless { row.isNull("banner_image") }
)
