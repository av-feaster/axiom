package com.toonandtools.axiom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.toonandtools.axiom.llama.LlamaEngine
import com.toonandtools.axiom.llama.PromptBuilder
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize LlamaEngine
        val initSuccess = LlamaEngine.init(this, "tinyllama.gguf")
        
        setContent {
            AxiomTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LlamaDemoScreen(
                        modifier = Modifier.padding(innerPadding),
                        isInitialized = initSuccess
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LlamaEngine.cleanup()
    }
}

@Composable
fun LlamaDemoScreen(
    modifier: Modifier = Modifier,
    isInitialized: Boolean
) {
    var prompt by remember { mutableStateOf("Write a short quote") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🦙 Llama.cpp Demo",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = if (isInitialized) "✅ Model loaded successfully" else "❌ Model failed to load",
            color = if (isInitialized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Prompt") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = {
                if (!isInitialized) return@Button
                
                isLoading = true
                scope.launch {
                    result = LlamaEngine.generate(prompt)
                    isLoading = false
                }
            },
            enabled = isInitialized && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) "Generating..." else "Generate")
        }
        
        if (result.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Result:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LlamaDemoScreenPreview() {
    AxiomTheme {
        LlamaDemoScreen(isInitialized = true)
    }
}