package com.axiom.models

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import com.axiom.core.Model
import com.axiom.core.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Default implementation of ModelManager using Android DownloadManager
 */
class DefaultModelManager(private val context: Context) : ModelManager {
    
    private val modelsDirectory = File(context.filesDir, "models")
    
    init {
        if (!modelsDirectory.exists()) {
            modelsDirectory.mkdirs()
        }
    }
    
    override suspend fun fetchRegistry(): List<Model> = withContext(Dispatchers.IO) {
        // TODO: Fetch from remote registry
        // For now, return a hardcoded list of models
        listOf(
            Model(
                id = "tinyllama-1.1b",
                name = "TinyLlama 1.1B",
                description = "Small, fast model for quick inference",
                size = 500 * 1024 * 1024L, // 500MB
                downloadUrl = "https://example.com/models/tinyllama.gguf",
                checksum = "sha256:abc123",
                architecture = "llama",
                quantization = "Q4_K_M",
                minRam = 3 * 1024 * 1024 * 1024L, // 3GB
                recommended = true
            ),
            Model(
                id = "tinymistral-0.2b",
                name = "TinyMistral 0.2B",
                description = "Very small model for low-end devices",
                size = 130 * 1024 * 1024L, // 130MB
                downloadUrl = "https://example.com/models/tinymistral.gguf",
                checksum = "sha256:def456",
                architecture = "mistral",
                quantization = "Q4_K_M",
                minRam = 2 * 1024 * 1024 * 1024L, // 2GB
                recommended = false
            )
        )
    }
    
    override suspend fun download(model: Model, progressCallback: ((Float) -> Unit)?): String = 
        withContext(Dispatchers.IO) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(model.downloadUrl))
                .setTitle("Downloading ${model.name}")
                .setDescription("AI Model")
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "${model.id}.gguf")
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            
            val downloadId = downloadManager.enqueue(request)
            
            // Wait for download to complete
            suspendCancellableCoroutine { continuation ->
                val query = DownloadManager.Query().setFilterById(downloadId)
                var completed = false
                
                while (!completed) {
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        
                        val status = cursor.getInt(statusIndex)
                        val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                        val bytesTotal = cursor.getLong(bytesTotalIndex)
                        
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            completed = true
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            continuation.resumeWith(Result.failure(Exception("Download failed")))
                            return@suspendCancellableCoroutine
                        } else {
                            val progress = if (bytesTotal > 0) bytesDownloaded.toFloat() / bytesTotal else 0f
                            progressCallback?.invoke(progress)
                        }
                    }
                    cursor.close()
                    
                    if (!completed) {
                        Thread.sleep(500)
                    }
                }
                
                // Get the downloaded file path
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    val localUri = cursor.getString(localUriIndex)
                    val file = File(Uri.parse(localUri).path!!)
                    
                    // Move to models directory
                    val targetFile = File(modelsDirectory, "${model.id}.gguf")
                    file.copyTo(targetFile, overwrite = true)
                    file.delete()
                    
                    continuation.resume(targetFile.absolutePath)
                }
                cursor.close()
            }
        }
    
    override fun getInstalledModels(): List<Model> {
        return modelsDirectory.listFiles()
            ?.filter { it.extension == "gguf" }
            ?.mapNotNull { file ->
                // TODO: Read model metadata from file
                null
            } ?: emptyList()
    }
    
    override suspend fun delete(model: Model): Boolean = withContext(Dispatchers.IO) {
        val file = File(modelsDirectory, "${model.id}.gguf")
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
    
    override fun recommend(context: Context): Model? {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val availableRam = memoryInfo.totalMem
        
        // Recommend based on available RAM
        return when {
            availableRam >= 6 * 1024 * 1024 * 1024L -> null // Could handle larger models
            availableRam >= 3 * 1024 * 1024 * 1024L -> Model(
                id = "tinyllama-1.1b",
                name = "TinyLlama 1.1B",
                description = "Small, fast model for quick inference",
                size = 500 * 1024 * 1024L,
                downloadUrl = "https://example.com/models/tinyllama.gguf",
                checksum = "sha256:abc123",
                architecture = "llama",
                quantization = "Q4_K_M",
                minRam = 3 * 1024 * 1024 * 1024L,
                recommended = true
            )
            else -> Model(
                id = "tinymistral-0.2b",
                name = "TinyMistral 0.2B",
                description = "Very small model for low-end devices",
                size = 130 * 1024 * 1024L,
                downloadUrl = "https://example.com/models/tinymistral.gguf",
                checksum = "sha256:def456",
                architecture = "mistral",
                quantization = "Q4_K_M",
                minRam = 2 * 1024 * 1024 * 1024L,
                recommended = false
            )
        }
    }
    
    override fun downloadProgress(model: Model): Flow<Float> = flow {
        // TODO: Implement progress streaming
        emit(0f)
    }
}
