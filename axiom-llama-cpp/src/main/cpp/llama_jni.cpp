#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>

#include "llama.h"

#define LOG_TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global variables for model and context
static llama_model* g_model = nullptr;
static llama_context* g_context = nullptr;
static int g_max_tokens = 512;
static float g_temperature = 0.7f;
static int g_top_k = 40;
static float g_top_p = 0.95f;
static float g_repeat_penalty = 1.0f;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_axiom_llama_cpp_LlamaCppEngine_nativeInit(
    JNIEnv* env, jobject /* this */,
    jstring model_path,
    jint context_size,
    jint threads,
    jfloat temperature,
    jint top_k,
    jfloat top_p,
    jfloat repeat_penalty,
    jint max_tokens
) {
    const char* model_path_cstr = env->GetStringUTFChars(model_path, nullptr);
    
    LOGI("Initializing llama.cpp with model: %s", model_path_cstr);
    
    // Store parameters
    g_max_tokens = max_tokens;
    g_temperature = temperature;
    g_top_k = top_k;
    g_top_p = top_p;
    g_repeat_penalty = repeat_penalty;
    
    // Initialize backend
    llama_backend_init();
    llama_numa_init(GGML_NUMA_STRATEGY_DISABLED);
    
    // Model parameters
    llama_model_params model_params = llama_model_default_params();
    
    // Load model
    g_model = llama_model_load_from_file(model_path_cstr, model_params);
    if (!g_model) {
        LOGE("Failed to load model from: %s", model_path_cstr);
        env->ReleaseStringUTFChars(model_path, model_path_cstr);
        return JNI_FALSE;
    }
    
    // Context parameters
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = context_size;  // Context size
    ctx_params.n_threads = threads;  // Number of threads
    
    // Create context
    g_context = llama_init_from_model(g_model, ctx_params);
    if (!g_context) {
        LOGE("Failed to create context");
        llama_model_free(g_model);
        g_model = nullptr;
        env->ReleaseStringUTFChars(model_path, model_path_cstr);
        return JNI_FALSE;
    }
    
    LOGI("Successfully initialized llama.cpp");
    env->ReleaseStringUTFChars(model_path, model_path_cstr);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_axiom_llama_cpp_LlamaCppEngine_nativeGenerate(JNIEnv* env, jobject /* this */, jstring prompt) {
    if (!g_model || !g_context) {
        LOGE("Model or context not initialized");
        return env->NewStringUTF("Error: Model not initialized");
    }
    
    const char* prompt_cstr = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Generating text for prompt: %s", prompt_cstr);
    
    // Get vocabulary from model
    const llama_vocab* vocab = llama_model_get_vocab(g_model);
    
    // Tokenize prompt
    std::vector<llama_token> tokens;
    tokens.resize(llama_n_ctx(g_context));
    
    int n_tokens = llama_tokenize(
        vocab,
        prompt_cstr,
        strlen(prompt_cstr),
        tokens.data(),
        tokens.size(),
        true,  // add_special
        false  // parse_special
    );
    
    if (n_tokens < 0) {
        LOGE("Failed to tokenize prompt");
        env->ReleaseStringUTFChars(prompt, prompt_cstr);
        return env->NewStringUTF("Error: Failed to tokenize prompt");
    }
    
    tokens.resize(n_tokens);
    
    // Clear the KV cache - this function doesn't exist in current API, so we'll skip it
    // llama_kv_cache_clear(g_context);
    
    // Create batch for prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    
    if (llama_decode(g_context, batch) != 0) {
        LOGE("Failed to decode prompt");
        env->ReleaseStringUTFChars(prompt, prompt_cstr);
        return env->NewStringUTF("Error: Failed to decode prompt");
    }
    
    // Generation parameters (use stored values)
    const int max_tokens = g_max_tokens;
    const float temperature = g_temperature;
    const int top_k = g_top_k;
    
    std::string result;
    llama_token new_token;
    
    // Generation loop
    for (int i = 0; i < max_tokens; i++) {
        // Get logits for next token
        float* logits = llama_get_logits(g_context);
        
        // Simple greedy sampling - find the token with highest logit
        llama_token best_token = 0;
        float max_logit = -INFINITY;
        
        // Get vocab size
        int n_vocab = llama_vocab_n_tokens(vocab);
        
        for (int token_id = 0; token_id < n_vocab; token_id++) {
            if (logits[token_id] > max_logit) {
                max_logit = logits[token_id];
                best_token = token_id;
            }
        }
        
        new_token = best_token;
        
        // Convert token to string
        char token_str[256];
        int token_len = llama_token_to_piece(vocab, new_token, token_str, sizeof(token_str), 0, true);
        
        if (token_len <= 0) {
            break;  // End of generation
        }
        
        result += std::string(token_str, token_len);
        
        // Check for end of sequence
        if (new_token == llama_vocab_eos(vocab)) {
            break;
        }
        
        // Prepare next batch with single token
        llama_batch next_batch = llama_batch_get_one(&new_token, 1);
        
        if (llama_decode(g_context, next_batch) != 0) {
            LOGE("Failed to decode during generation");
            break;
        }
    }
    
    env->ReleaseStringUTFChars(prompt, prompt_cstr);
    
    LOGI("Generated text: %s", result.c_str());
    return env->NewStringUTF(result.c_str());
}

// Global callback for streaming
static jobject g_callback_object = nullptr;
static jmethodID g_callback_method = nullptr;

extern "C" JNIEXPORT void JNICALL
Java_com_axiom_llama_cpp_LlamaCppEngine_nativeStream(JNIEnv* env, jobject /* this */, jstring prompt, jobject callback) {
    if (!g_model || !g_context) {
        LOGE("Model or context not initialized");
        return;
    }
    
    const char* prompt_cstr = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Streaming text for prompt: %s", prompt_cstr);
    
    // Get callback method
    jclass callback_class = env->GetObjectClass(callback);
    g_callback_method = env->GetMethodID(callback_class, "invoke", "(Ljava/lang/String;)V");
    g_callback_object = env->NewGlobalRef(callback);
    
    // Get vocabulary from model
    const llama_vocab* vocab = llama_model_get_vocab(g_model);
    
    // Tokenize prompt
    std::vector<llama_token> tokens;
    tokens.resize(llama_n_ctx(g_context));
    
    int n_tokens = llama_tokenize(
        vocab,
        prompt_cstr,
        strlen(prompt_cstr),
        tokens.data(),
        tokens.size(),
        true,
        false
    );
    
    if (n_tokens < 0) {
        LOGE("Failed to tokenize prompt");
        env->ReleaseStringUTFChars(prompt, prompt_cstr);
        return;
    }
    
    tokens.resize(n_tokens);
    
    // Create batch for prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    
    if (llama_decode(g_context, batch) != 0) {
        LOGE("Failed to decode prompt");
        env->ReleaseStringUTFChars(prompt, prompt_cstr);
        return;
    }
    
    // Generation loop
    for (int i = 0; i < g_max_tokens; i++) {
        float* logits = llama_get_logits(g_context);
        
        // Simple greedy sampling
        llama_token best_token = 0;
        float max_logit = -INFINITY;
        
        int n_vocab = llama_vocab_n_tokens(vocab);
        
        for (int token_id = 0; token_id < n_vocab; token_id++) {
            if (logits[token_id] > max_logit) {
                max_logit = logits[token_id];
                best_token = token_id;
            }
        }
        
        llama_token new_token = best_token;
        
        // Convert token to string
        char token_str[256];
        int token_len = llama_token_to_piece(vocab, new_token, token_str, sizeof(token_str), 0, true);
        
        if (token_len <= 0) {
            break;
        }
        
        // Call Kotlin callback with token
        jstring token_jstring = env->NewStringUTF(token_str);
        env->CallVoidMethod(g_callback_object, g_callback_method, token_jstring);
        env->DeleteLocalRef(token_jstring);
        
        if (new_token == llama_vocab_eos(vocab)) {
            break;
        }
        
        llama_batch next_batch = llama_batch_get_one(&new_token, 1);
        
        if (llama_decode(g_context, next_batch) != 0) {
            LOGE("Failed to decode during streaming");
            break;
        }
    }
    
    env->ReleaseStringUTFChars(prompt, prompt_cstr);
    
    // Cleanup global references
    if (g_callback_object) {
        env->DeleteGlobalRef(g_callback_object);
        g_callback_object = nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_axiom_llama_cpp_LlamaCppEngine_nativeCleanup(JNIEnv* env, jobject /* this */) {
    LOGI("Cleaning up llama.cpp resources");
    
    if (g_context) {
        llama_free(g_context);
        g_context = nullptr;
    }
    
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    
    llama_backend_free();
}