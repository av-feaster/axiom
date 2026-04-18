# 🦙 Llama.cpp Android Integration

This project demonstrates the integration of llama.cpp for on-device LLM inference in an Android app.

## 📁 Project Structure

```
app/src/main/
├── cpp/
│   ├── CMakeLists.txt              # CMake configuration
│   ├── llama_jni.cpp               # JNI bridge implementation
│   └── llama.cpp/                  # llama.cpp submodule
├── assets/
│   └── models/
│       └── tinyllama.gguf          # TinyLlama model (667MB)
└── java/com/toonandtools/axiom/llama/
    ├── LlamaEngine.kt              # Main API object
    ├── ModelLoader.kt              # Asset management
    └── PromptBuilder.kt            # Prompt utilities
```

## � Model Download

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

3. **Rebuild the app**:
   ```bash
   ./gradlew assembleDebug
   ```

## �🚀 Usage Example

```kotlin
// Initialize the engine
val success = LlamaEngine.init(context, "tinyllama.gguf")

// Generate text
val result = LlamaEngine.generate("Write a short quote")

// Cleanup when done
LlamaEngine.cleanup()
```

## ⚙️ Setup Requirements

### 1. Install Android NDK

The project requires Android NDK version 26.1.10909125 or later.

**Option A: Using Android Studio**
1. Open Android Studio
2. Go to Tools → SDK Manager
3. Select "SDK Tools" tab
4. Check "NDK (Side by side)"
5. Click Apply to install

**Option B: Using Command Line**
```bash
# Add to local.properties
echo "ndk.dir=/Users/aman.verma/Library/Android/sdk/ndk/26.1.10909125" >> local.properties

# Or install via sdkmanager (if available)
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "ndk;26.1.10909125"
```

### 2. Build the Project

```bash
./gradlew assembleDebug
```

## 🔧 Technical Implementation

### JNI Bridge (`llama_jni.cpp`)

- **nativeInit**: Initializes llama.cpp backend, loads GGUF model, creates context
- **nativeGenerate**: Tokenizes prompt, runs inference loop, samples tokens
- **nativeCleanup**: Frees model and context resources

### Key Parameters

- **Context Size**: 512 tokens
- **Max Generation**: 64 tokens
- **Temperature**: 0.7
- **Top-K**: 40
- **Threads**: 4

### Kotlin API

#### LlamaEngine
```kotlin
object LlamaEngine {
    fun init(context: Context, modelAssetName: String): Boolean
    suspend fun generate(prompt: String): String
    fun isInitialized(): Boolean
    fun cleanup()
}
```

#### ModelLoader
```kotlin
object ModelLoader {
    fun prepare(context: Context, assetName: String): String
    fun modelExists(context: Context, assetName: String): Boolean
    fun getModelSize(context: Context, assetName: String): Long
    fun deleteModel(context: Context, assetName: String): Boolean
}
```

#### PromptBuilder
```kotlin
object PromptBuilder {
    fun quote(input: String): String
    fun instruction(instruction: String): String
    fun questionAnswer(question: String): String
    fun completion(prefix: String): String
    fun chat(userMessage: String): String
}
```

## 📱 Demo App

The MainActivity includes a complete demo UI that:

1. ✅ Shows model initialization status
2. 📝 Provides text input for prompts
3. 🔄 Generates responses with loading indicator
4. 📋 Displays generated results
5. 🧹 Properly cleans up resources on exit

## 🎯 Performance Considerations

- **Model Loading**: ~2-5 seconds (one-time)
- **Generation**: ~1-3 seconds per 64 tokens
- **Memory Usage**: ~500MB for TinyLlama model
- **Storage**: 667MB for model file

## 🐛 Troubleshooting

### Build Issues

**Error**: `NDK not configured`
- **Solution**: Install NDK as described above

**Error**: `llama.h not found`
- **Solution**: Ensure llama.cpp submodule is properly initialized

### Runtime Issues

**Error**: `Failed to load model`
- **Solution**: Check that tinyllama.gguf exists in assets/models/

**Error**: `Generation failed`
- **Solution**: Check logcat for JNI error messages

## 🔍 Monitoring

Use logcat to monitor JNI operations:

```bash
adb logcat | grep LlamaJNI
```

## 📈 Next Steps

1. **Performance Optimization**: Implement quantization support
2. **Model Management**: Add model downloading/caching
3. **Advanced Sampling**: Implement temperature, top-p, repetition penalty
4. **Streaming**: Add real-time token streaming
5. **Memory Management**: Implement context window management

## 📄 License

This integration follows the llama.cpp license terms. The TinyLlama model is provided under the Apache 2.0 license.
