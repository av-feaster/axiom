# Log Reader Guide

## Overview
This document explains how to read and interpret the logs from the Axiom AI SDK Android application.

## Log Tags

The application uses structured logging with the following tags to categorize log messages:

| Tag | Module | Purpose |
|-----|--------|---------|
| `MainActivity` | sample | General activity lifecycle events |
| `NetworkMonitor` | sample | Network connectivity status changes |
| `ModelDownload` | sample | Model download progress and status |
| `TextGeneration` | sample | Text generation and streaming events |
| `EngineInit` | sample | Engine initialization and cleanup |
| `ModelManager` | axiom-models | Model manager initialization and directory setup |
| `ModelRegistry` | axiom-models | Model registry fetching and listing |
| `LlamaCppEngine` | axiom-llama-cpp | Llama.cpp engine lifecycle and operations |
| `LlamaJNI` | axiom-llama-cpp (native) | Native C++ JNI layer logs |

## Log Levels

- **VERBOSE** (`Log.v`): Detailed debugging information
- **DEBUG** (`Log.d`): Debugging information for development
- **INFO** (`Log.i`): General informational messages
- **WARN** (`Log.w`): Warning messages for potentially harmful situations
- **ERROR** (`Log.e`): Error messages for failures and exceptions

## Reading Logs in Android Studio

### Method 1: Logcat
1. Open Android Studio
2. Go to **View → Tool Windows → Logcat**
3. Select your device/emulator from the dropdown
4. Filter by package: `com.axiom.sample`
5. Filter by tag: e.g., `tag:NetworkMonitor` or `tag:ModelDownload`

### Method 2: Command Line
```bash
# View all logs for the app
adb logcat -s com.axiom.sample

# Filter by specific tag
adb logcat -s NetworkMonitor:V ModelDownload:V TextGeneration:V EngineInit:V MainActivity:V

# Save logs to file
adb logcat -s com.axiom.sample > app_logs.txt
```

## Common Log Scenarios

### 1. Network Connectivity
```
I/NetworkMonitor: Setting up network connectivity monitoring
I/NetworkMonitor: Initial network state: connected=true
I/NetworkMonitor: Network available - Internet connected
W/NetworkMonitor: Network lost - No internet connected
```

### 2. Model Download
```
I/ModelDownload: Checking for existing model at: /data/data/.../models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
I/ModelDownload: File exists: false, size: 0
I/ModelDownload: Model not found or too small, starting download
D/ModelDownload: Download progress: 21%
D/ModelDownload: Download progress: 45%
I/ModelDownload: Download completed, path: /data/data/.../models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
```

### 3. Engine Initialization
```
I/EngineInit: initializeEngine called for model: tinyllama-1.1b-chat-v1.0.Q4_K_M
I/EngineInit: Model name: TinyLlama 1.1B Chat
I/EngineInit: Model URL: https://huggingface.co/...
I/EngineInit: Model size: 673886208 bytes
I/EngineInit: Model path resolved to: /data/data/.../models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
I/EngineInit: Initializing engine with config: contextSize=1024, temperature=0.7
I/EngineInit: Engine init result: true
I/EngineInit: Engine initialized successfully
```

### 4. Text Generation (Streaming)
```
I/TextGeneration: Starting text generation (streaming=true)
D/TextGeneration: Prompt length: 42 chars
I/TextGeneration: Using streaming generation
D/TextGeneration: Streamed 10 tokens
D/TextGeneration: Streamed 20 tokens
I/TextGeneration: Streaming complete: 35 tokens generated
```

### 5. Text Generation (Non-Streaming)
```
I/TextGeneration: Starting text generation (streaming=false)
D/TextGeneration: Prompt length: 42 chars
I/TextGeneration: Using non-streaming generation
I/TextGeneration: Generation complete: 145 chars generated
```

### 6. SDK Logging Examples

#### Model Manager (axiom-models)
```
I/ModelManager: Initializing DefaultModelManager
D/ModelManager: Models directory: /data/data/.../files/models
I/ModelManager: Created models directory
I/ModelRegistry: Fetching model registry
I/ModelRegistry: Registry fetched: 2 models available
D/ModelRegistry:   - tinyllama-1.1b: TinyLlama 1.1B Chat (699050496 bytes)
D/ModelRegistry:   - qwen2.5-0.5b-instruct: Qwen2.5 0.5B Instruct (417021952 bytes)
```

#### LlamaCppEngine (axiom-llama-cpp)
```
I/LlamaCppEngine: Loading native library: llama_jni
I/LlamaCppEngine: Native library loaded successfully
I/LlamaCppEngine: Initializing LlamaCppEngine
D/LlamaCppEngine: Model path: /data/data/.../models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
D/LlamaCppEngine: Context size: 1024
D/LlamaCppEngine: Threads: 4
D/LlamaCppEngine: Temperature: 0.7
I/LlamaCppEngine: LlamaCppEngine initialized successfully
I/LlamaCppEngine: Starting streaming generation
D/LlamaCppEngine: Prompt length: 42 chars
D/LlamaCppEngine: Streamed 10 tokens
D/LlamaCppEngine: Streamed 20 tokens
I/LlamaCppEngine: Streaming completed: 35 tokens
```

#### Native JNI (LlamaJNI)
```
I/LlamaJNI: Initializing llama.cpp with model: /data/data/.../models/...
I/LlamaJNI: Successfully initialized llama.cpp
```

### 7. Errors
```
E/ModelDownload: Download exception
E/ModelDownload: java.io.IOException: Connection timeout
E/EngineInit: Engine initialization failed
E/TextGeneration: Error during generation
E/TextGeneration: java.lang.IllegalArgumentException: Invalid UTF-8 sequence
```

## Troubleshooting Guide

### Download Stuck at 0%
- Check `NetworkMonitor` logs to confirm internet connectivity
- Verify `ModelDownload` logs for error messages
- Check if URL is accessible

### Engine Initialization Failed
- Check `EngineInit` logs for model path
- Verify model file size is > 1MB
- Check for JNI errors in logcat

### Generation Not Working
- Check `TextGeneration` logs for initialization status
- Verify engine is initialized (`isInitialized=true`)
- Check for UTF-8 encoding errors

### Network Snackbar Not Showing
- Check `NetworkMonitor` logs for network state changes
- Verify `ACCESS_NETWORK_STATE` permission in manifest
- Check if initial network state is detected correctly

## JNI Native Logs

The native C++ code also logs with the `LlamaJNI` tag:
```
I/LlamaJNI: Initializing llama.cpp with model: /data/data/.../models/...
I/LlamaJNI: Successfully initialized llama.cpp
```

## Filtering Tips

### Filter by multiple tags
```
tag:NetworkMonitor | tag:ModelDownload
```

### Filter by log level
```
level:error | level:warn
```

### Filter by package and tag
```
package:com.axiom.sample tag:TextGeneration
```

## Log File Locations

- **Android Studio Logcat**: Real-time viewing
- **ADB**: `adb logcat` for command line access
- **Device**: `/data/local/tmp/` (if accessible)
- **Crash logs**: `/data/tombstones/` (native crashes)

## Best Practices

1. Always check log level when debugging (set to VERBOSE for detailed logs)
2. Use tag filtering to focus on specific components
3. Look for ERROR and WARN logs first when troubleshooting
4. Check the sequence of INFO logs to understand the flow
5. Native JNI errors may appear with different tags like `LlamaJNI`

## Additional Resources

- [Android Logcat Documentation](https://developer.android.com/studio/debug/logcat)
- [ADB Logcat Commands](https://developer.android.com/studio/command-line/logcat)
