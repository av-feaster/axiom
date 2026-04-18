# Llama.cpp Android SDK

A production-ready Android SDK (AAR) for on-device LLM inference using llama.cpp. This SDK provides a clean Kotlin API for running LLM models directly on Android devices without requiring cloud services.

## 📦 SDK Information

- **AAR File**: `app/build/outputs/aar/app-release.aar`
- **Size**: ~658MB (includes llama.cpp native libraries)
- **Architecture**: arm64-v8a only (optimized for modern Android devices)
- **Min SDK**: 24 (Android 7.0)
- **Compile SDK**: 36 (Android 14)

## 🚀 Quick Start

### 1. Add the AAR to your project

Copy `app-release.aar` to your project's `libs` directory:

```bash
mkdir -p app/libs
cp app-release.aar app/libs/
```

### 2. Add to your app's build.gradle.kts

```kotlin
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
}
```

### 3. Add model to assets

The model file is not included in the repository due to its size. To run the demo:

1. **Download TinyLlama 1.1B (Q4_K_M)**:
   ```bash
   wget https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
   ```

2. **Add to app assets**:
   ```bash
   mkdir -p app/src/main/assets/models
   cp tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf app/src/main/assets/models/tinyllama.gguf
   ```

### 4. Use the SDK in your code

```kotlin
import com.toonandtools.axiom.llama.LlamaEngine
import com.toonandtools.axiom.llama.ModelLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MyViewModel : ViewModel() {
    suspend fun initLlama(context: Context): Boolean = withContext(Dispatchers.IO) {
        // Copy model from assets to files directory
        val modelPath = ModelLoader.copyModelToFilesDir(context, "your-model.gguf")
        
        // Initialize the engine
        LlamaEngine.init(context, modelPath.absolutePath)
    }
    
    suspend fun generateText(prompt: String): String = withContext(Dispatchers.IO) {
        LlamaEngine.generate(prompt)
    }
    
    fun cleanup() {
        LlamaEngine.cleanup()
    }
}
```

## 📚 API Reference

### LlamaEngine

Main entry point for llama.cpp operations.

```kotlin
object LlamaEngine {
    /**
     * Initialize the llama.cpp engine with a model
     * @param context Application context
     * @param modelPath Absolute path to the GGUF model file
     * @return true if initialization succeeded, false otherwise
     */
    suspend fun init(context: Context, modelPath: String): Boolean
    
    /**
     * Generate text from a prompt
     * @param prompt Input text prompt
     * @return Generated text response
     */
    suspend fun generate(prompt: String): String
    
    /**
     * Cleanup and release resources
     */
    fun cleanup()
}
```

### ModelLoader

Utility for managing model files.

```kotlin
object ModelLoader {
    /**
     * Copy a model from assets to the app's files directory
     * @param context Application context
     * @param assetName Name of the model file in assets
     * @return File object pointing to the copied model
     */
    suspend fun copyModelToFilesDir(context: Context, assetName: String): File
    
    /**
     * Check if a model file exists in the files directory
     * @param context Application context
     * @param fileName Name of the model file
     * @return true if the model exists
     */
    fun modelExists(context: Context, fileName: String): Boolean
    
    /**
     * Get the file path for a model in the files directory
     * @param context Application context
     * @param fileName Name of the model file
     * @return File object pointing to the model
     */
    fun getModelPath(context: Context, fileName: String): File
}
```

### PromptBuilder

Utility for formatting prompts.

```kotlin
object PromptBuilder {
    /**
     * Build a simple prompt
     * @param instruction The instruction for the model
     * @return Formatted prompt string
     */
    fun buildPrompt(instruction: String): String
    
    /**
     * Build a prompt with context
     * @param context Additional context for the model
     * @param instruction The instruction for the model
     * @return Formatted prompt string
     */
    fun buildPromptWithContext(context: String, instruction: String): String
    
    /**
     * Build a chat-style prompt
     * @param conversation List of (user, assistant) message pairs
     * @return Formatted chat prompt
     */
    fun buildChatPrompt(conversation: List<Pair<String, String>>): String
}
```

## 🔧 Configuration

### Model Requirements

- **Format**: GGUF (Quantized model format)
- **Recommended Size**: < 2GB for optimal performance
- **Supported Models**: Any GGUF-compatible model (Llama, Mistral, etc.)

### Performance Tuning

The SDK uses the following default parameters (can be modified in `llama_jni.cpp`):

- **Context Size**: 512 tokens
- **Threads**: 4
- **Max Generation Tokens**: 64
- **Sampling**: Greedy (can be extended to temperature/top-k)

### Memory Requirements

- **Model Size**: ~500MB for a 7B quantized model
- **Runtime Memory**: ~200-300MB additional for context
- **Total**: ~700-800MB for a typical setup

## 📋 Example Implementation

### Complete Example with Activity

```kotlin
class MainActivity : AppCompatActivity() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModelScope.launch {
            // Initialize engine
            val success = withContext(Dispatchers.IO) {
                val modelPath = ModelLoader.copyModelToFilesDir(this@MainActivity, "tinyllama.gguf")
                LlamaEngine.init(this@MainActivity, modelPath.absolutePath)
            }
            
            if (success) {
                // Generate text
                val result = withContext(Dispatchers.IO) {
                    LlamaEngine.generate("Write a short poem about Android")
                }
                println("Generated: $result")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LlamaEngine.cleanup()
        viewModelScope.cancel()
    }
}
```

### Using with Jetpack Compose

```kotlin
@Composable
fun LlamaScreen() {
    var prompt by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Prompt") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    response = withContext(Dispatchers.IO) {
                        LlamaEngine.generate(prompt)
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("Generate")
            }
        }
        
        if (response.isNotEmpty()) {
            Text(
                text = response,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

## ⚠️ Important Notes

### Thread Safety

- `LlamaEngine.init()` and `LlamaEngine.generate()` are thread-safe when called from coroutines
- `LlamaEngine.cleanup()` should be called from the main thread or with proper synchronization

### Lifecycle Management

- Always call `LlamaEngine.cleanup()` when done to release native resources
- Initialize the engine only once per application lifecycle
- Consider initializing in Application class or a singleton

### Model Loading

- Models are copied from assets to files directory on first use
- This can take time for large models, consider doing it in a coroutine
- Models persist in files directory after initial copy

### Performance Considerations

- First generation after initialization may be slower
- Subsequent generations are faster with warm cache
- Consider keeping the engine initialized for repeated use
- Avoid frequent init/cleanup cycles

## 🔍 Troubleshooting

### Build Issues

If you encounter build errors when adding the AAR:

1. Ensure you have the required dependencies (kotlinx-coroutines, androidx.core, androidx.lifecycle)
2. Make sure your project uses Kotlin 2.0.21 or higher
3. Verify your project's compile SDK is at least 36

### Runtime Issues

**"Failed to load model"**
- Verify the model file is a valid GGUF format
- Check the model file path is correct
- Ensure you have sufficient storage space

**"Failed to generate text"**
- Check if the prompt is valid
- Verify the model was initialized successfully
- Check Android logcat for detailed error messages

**Out of Memory**
- Try a smaller model (more quantization)
- Reduce context size in llama_jni.cpp
- Close other apps to free memory

## 📝 License

This SDK includes llama.cpp which is licensed under the MIT License. The SDK wrapper code is also provided under the MIT License.

## 🤝 Contributing

To modify or extend this SDK:

1. Clone this repository
2. Modify the Kotlin API in `app/src/main/java/com/toonandtools/axiom/llama/`
3. Modify the JNI layer in `app/src/main/cpp/llama_jni.cpp`
4. Rebuild the AAR: `./gradlew assembleRelease`
5. The new AAR will be in `app/build/outputs/aar/app-release.aar`

## 📞 Support

For issues or questions:
- Check the llama.cpp documentation: https://github.com/ggerganov/llama.cpp
- Review this SDK's source code for implementation details
- Check Android logcat for runtime errors
