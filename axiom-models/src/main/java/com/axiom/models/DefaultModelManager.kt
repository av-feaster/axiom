package com.axiom.models

import android.content.Context
import android.util.Log
import com.axiom.core.Model
import com.axiom.core.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Public Hugging Face GGUF URLs (llama.cpp loads local files only; sample app downloads these first). */
private fun builtInRegistry(): List<Model> = listOf(
    Model(
        id = "tinyllama-1.1b",
        name = "TinyLlama 1.1B Chat",
        description = "Small, fast chat model (Q4_K_M GGUF from Hugging Face)",
        size = 667L * 1024 * 1024,
        downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
        checksum = "",
        architecture = "llama",
        quantization = "Q4_K_M",
        minRam = 3L * 1024 * 1024 * 1024,
        recommended = true
    ),
    Model(
        id = "qwen2.5-0.5b-instruct",
        name = "Qwen2.5 0.5B Instruct",
        description = "Very small instruct model for low-RAM devices (Q4_K_M GGUF)",
        size = 398L * 1024 * 1024,
        downloadUrl = "https://huggingface.co/lmstudio-community/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
        checksum = "",
        architecture = "qwen2",
        quantization = "Q4_K_M",
        minRam = 2L * 1024 * 1024 * 1024,
        recommended = false
    )
)

/**
 * Default implementation of ModelManager using Android DownloadManager
 */
class DefaultModelManager(private val context: Context) : ModelManager {
    
    companion object {
        private const val TAG = "ModelManager"
        private const val TAG_DOWNLOAD = "ModelDownload"
        private const val TAG_REGISTRY = "ModelRegistry"
    }
    
    private val modelsDirectory = File(context.filesDir, "models")
    
    init {
        Log.i(TAG, "Initializing DefaultModelManager")
        Log.d(TAG, "Models directory: ${modelsDirectory.absolutePath}")
        if (!modelsDirectory.exists()) {
            modelsDirectory.mkdirs()
            Log.i(TAG, "Created models directory")
        } else {
            Log.d(TAG, "Models directory already exists")
        }
    }
    
    override suspend fun fetchRegistry(): List<Model> = withContext(Dispatchers.IO) {
        Log.i(TAG_REGISTRY, "Fetching model registry")
        val models = builtInRegistry()
        Log.i(TAG_REGISTRY, "Registry fetched: ${models.size} models available")
        models.forEach { model ->
            Log.d(TAG_REGISTRY, "  - ${model.id}: ${model.name} (${model.size} bytes)")
        }
        models
    }
    
    override suspend fun download(model: Model, progressCallback: ((Float) -> Unit)?): String =
        withContext(Dispatchers.IO) {
            Log.i(TAG_DOWNLOAD, "Starting HTTP download for model: ${model.id}")
            Log.i(TAG_DOWNLOAD, "Model name: ${model.name}")
            Log.d(TAG_DOWNLOAD, "Download URL: ${model.downloadUrl}")
            Log.d(TAG_DOWNLOAD, "Expected size: ${model.size} bytes")

            val targetFile = File(modelsDirectory, "${model.id}.gguf")
            val tempFile = File(modelsDirectory, "${model.id}.gguf.part")
            if (tempFile.exists()) {
                Log.d(TAG_DOWNLOAD, "Deleting existing temp file")
                tempFile.delete()
            }

            try {
                Log.d(TAG_DOWNLOAD, "Opening HTTP connection")
                val connection = (URL(model.downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    // Large GGUF: do not time out between chunks (DownloadManager was unreliable for HF).
                    readTimeout = 0
                    requestMethod = "GET"
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 AxiomSample/1.0"
                    )
                    setRequestProperty("Accept", "*/*")
                }

                connection.connect()
                val code = connection.responseCode
                Log.d(TAG_DOWNLOAD, "HTTP response code: $code")
                
                if (code !in 200..299) {
                    val errBody = connection.errorStream?.use { it.readBytes().decodeToString() }?.take(400) ?: ""
                    connection.disconnect()
                    Log.e(TAG_DOWNLOAD, "HTTP error: $code - ${connection.responseMessage}")
                    Log.e(TAG_DOWNLOAD, "Error body: $errBody")
                    throw Exception("HTTP $code: ${connection.responseMessage} $errBody")
                }

                val contentLength = connection.contentLengthLong
                val totalForProgress = when {
                    contentLength > 0L -> {
                        Log.d(TAG_DOWNLOAD, "Content-Length header: $contentLength bytes")
                        contentLength
                    }
                    model.size > 0L -> {
                        Log.d(TAG_DOWNLOAD, "Using model size: ${model.size} bytes")
                        model.size
                    }
                    else -> {
                        Log.w(TAG_DOWNLOAD, "No size information available")
                        -1L
                    }
                }

                Log.i(TAG_DOWNLOAD, "Starting download stream")
                connection.inputStream.buffered().use { input ->
                    tempFile.outputStream().buffered().use { output ->
                        val buf = ByteArray(512 * 1024)
                        var downloaded = 0L
                        var lastProgressEmit = 0L
                        var lastLogEmit = 0L
                        while (true) {
                            ensureActive()
                            val n = input.read(buf)
                            if (n == -1) break
                            if (n == 0) continue
                            output.write(buf, 0, n)
                            downloaded += n
                            
                            // Emit progress callback every 256KB
                            if (totalForProgress > 0L && downloaded - lastProgressEmit >= 256 * 1024L) {
                                lastProgressEmit = downloaded
                                val p = (downloaded.toFloat() / totalForProgress.toFloat()).coerceIn(0f, 1f)
                                progressCallback?.invoke(p)
                            }
                            
                            // Log progress every 10MB
                            if (downloaded - lastLogEmit >= 10 * 1024 * 1024L) {
                                lastLogEmit = downloaded
                                val progress = if (totalForProgress > 0L) {
                                    String.format("%.1f%%", (downloaded.toFloat() / totalForProgress * 100))
                                } else {
                                    "${downloaded / 1024 / 1024}MB"
                                }
                                Log.d(TAG_DOWNLOAD, "Download progress: $progress ($downloaded bytes)")
                            }
                        }
                        if (totalForProgress > 0L) {
                            progressCallback?.invoke(1f)
                        }
                        Log.i(TAG_DOWNLOAD, "Download stream completed: $downloaded bytes")
                    }
                }
                connection.disconnect()

                if (targetFile.exists()) {
                    Log.d(TAG_DOWNLOAD, "Deleting existing target file")
                    targetFile.delete()
                }
                
                if (!tempFile.renameTo(targetFile)) {
                    Log.w(TAG_DOWNLOAD, "Rename failed, using copy instead")
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                if (!targetFile.isFile || targetFile.length() < 100_000L) {
                    Log.e(TAG_DOWNLOAD, "Downloaded file is missing or too small (${targetFile.length()} bytes)")
                    throw Exception("Downloaded file is missing or too small (${targetFile.length()} bytes)")
                }

                Log.i(TAG_DOWNLOAD, "Download finished: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                targetFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG_DOWNLOAD, "Download failed for ${model.id}", e)
                tempFile.delete()
                throw e
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
        
        val models = builtInRegistry()
        // Recommend based on available RAM
        return when {
            availableRam >= 6 * 1024 * 1024 * 1024L -> null // Could handle larger models
            availableRam >= 3 * 1024 * 1024 * 1024L -> models[0]
            else -> models[1]
        }
    }
    
    override fun downloadProgress(model: Model): Flow<Float> = flow {
        // TODO: Implement progress streaming
        emit(0f)
    }
}
