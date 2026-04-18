package com.toonandtools.axiom.llama

object PromptBuilder {
    
    /**
     * Create a properly quoted prompt
     * @param input Input string to quote
     * @return Quoted string
     */
    fun quote(input: String): String {
        return if (input.startsWith("\"") && input.endsWith("\"")) {
            input
        } else {
            "\"$input\""
        }
    }
    
    /**
     * Create a basic instruction prompt
     * @param instruction The instruction to follow
     * @return Formatted prompt
     */
    fun instruction(instruction: String): String {
        return "Instruction: $instruction\nResponse:"
    }
    
    /**
     * Create a question-answer prompt
     * @param question The question to answer
     * @return Formatted prompt
     */
    fun questionAnswer(question: String): String {
        return "Question: $question\nAnswer:"
    }
    
    /**
     * Create a completion prompt
     * @param prefix Text to complete
     * @return Formatted prompt
     */
    fun completion(prefix: String): String {
        return "Complete the following text:\n$prefix"
    }
    
    /**
     * Create a chat-style prompt
     * @param userMessage User message
     * @return Formatted prompt
     */
    fun chat(userMessage: String): String {
        return "User: $userMessage\nAssistant:"
    }
}
