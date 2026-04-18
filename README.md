# Axiom AI - On-Device AI Runtime for Android

> Run ChatGPT-like features **fully offline** on Android in minutes.
> No APIs. No cloud. No data leakage.

---

## ✨ Why Axiom AI?

Most AI SDKs require:

* ❌ Cloud APIs
* ❌ Latency
* ❌ Privacy tradeoffs
* ❌ Expensive scaling

**Axiom AI flips that:**

* ✅ 100% on-device inference (powered by llama.cpp)
* ✅ Offline-first (works in airplane mode)
* ✅ Kotlin-first clean API
* ✅ Play Store–safe model delivery
* ✅ Production-ready architecture

---

## 📦 What You Get

* 🧠 Local LLM inference (GGUF models)
* ⚡ Streaming text generation
* 📥 Built-in model download manager
* 🧩 Modular architecture
* 📱 Android-optimized performance

---

## 🎥 Demo

> Offline AI running on a real Android device (no internet)

*Add demo GIF/video here*

---

## ⚡ Quick Start (1 Minute)

### 1. Add dependencies

```kotlin
dependencies {
    implementation("com.axiom:axiom-core:0.1.0")
    implementation("com.axiom:axiom-llama-cpp:0.1.0")
    implementation("com.axiom:axiom-models:0.1.0")
}
```

---

### 2. Initialize engine

```kotlin
val engine = LlamaCppEngine()
val modelManager = DefaultModelManager(context)

// Get recommended model
val model = modelManager.recommend(context)

// Download if needed
val modelPath = modelManager.download(model) { progress ->
    Log.d("Axiom", "Downloading: $progress")
}

// Initialize engine
engine.init(LLMConfig(
    modelPath = modelPath,
    contextSize = 1024,
    temperature = 0.7f,
    topK = 40
))
```

---

### 3. Generate text (streaming)

```kotlin
engine.stream("Explain Android in simple terms") { token ->
    print(token)
}
```

---

### Alternative: Manual Model Setup

For the sample app, you can manually download and add a model:

1. **Download TinyLlama 1.1B (Q4_K_M)**:
   ```bash
   wget https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
   ```

2. **Add to sample app assets**:
   ```bash
   mkdir -p sample/src/main/assets/models
   cp tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf sample/src/main/assets/models/tinyllama.gguf
   ```

3. **Rebuild sample app**:
   ```bash
   ./gradlew :sample:assembleDebug
   ```

---

## 🧠 Architecture

```
axiom-core         → SDK interfaces + config
axiom-llama-cpp    → llama.cpp engine binding
axiom-models       → model manager + downloader
sample-app         → demo implementation
```

---

## 📥 Model Delivery (Play Store Safe)

Axiom does **NOT bundle models in APK**.

Instead:

* Models are fetched from a remote registry
* Downloaded via Android DownloadManager
* Stored in app-private storage
* Verified via checksum

✅ No APK bloat
✅ No policy violations
✅ Resumable downloads

---

## 📚 Supported Models

| Model            | Size   | RAM   | Speed                    |
| ---------------- | ------ | ----- | ------------------------ |
| TinyLlama 1.1B   | ~500MB | 3–4GB | ⚡ Fast                   |
| TinyMistral 0.2B | ~130MB | 2–3GB | ⚡⚡ Very Fast             |
| Mistral 7B       | ~4GB   | 6–8GB | 🐢 Slow but high quality |

---

## 🧩 API Overview

### Engine

```kotlin
interface LLMEngine {
    suspend fun init(config: LLMConfig): Boolean
    suspend fun generate(prompt: String): String
    suspend fun stream(prompt: String, onToken: (String) -> Unit)
    fun cleanup()
}
```

---

### Config

```kotlin
data class LLMConfig(
    val modelPath: String,
    val contextSize: Int = 1024,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val threads: Int = Runtime.getRuntime().availableProcessors()
)
```

---

### Model Manager

```kotlin
ModelManager.fetchRegistry()

ModelManager.download(model) { progress -> }

ModelManager.getInstalledModels()

ModelManager.delete(model)
```

---

## ⚙️ Performance Tips

* Use **Q4_K_M quantization**
* Keep context ≤ 1024 for mobile
* Prefer small models for UX
* Avoid frequent init/cleanup

---

## 🔐 Privacy

* All inference runs locally
* No data leaves device
* No API calls

---

## 🛣 Roadmap

### v0.2

* [ ] Streaming API improvements
* [ ] Model auto-selection

### v0.3

* [ ] GPU / NNAPI acceleration
* [ ] Background downloads

### v1.0

* [ ] Stable SDK
* [ ] Production benchmarks
* [ ] UI components (ChatKit)

---

## 🤝 Contributing

We welcome contributions!

### Good first issues:

* Add new model configs
* Improve streaming API
* Optimize memory usage
* Enhance sample app UI

### Setup

```bash
git clone https://github.com/av-feaster/axiom
cd axiom
./gradlew build
```

---

## 📜 License

MIT License

---

## 💬 Vision

Axiom AI aims to become:

> **"Firebase for on-device AI"**

---

## ⭐ Support

If this project helps you:

* ⭐ Star the repo
* 🧵 Share on Twitter / Reddit
* 🧪 Build something cool

---

## 🧠 Final Thought

The future of AI is not just in the cloud.

It's **on your device**.

---

Made with ❤️ for indie developers
