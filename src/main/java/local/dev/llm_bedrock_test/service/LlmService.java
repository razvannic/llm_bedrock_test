package local.dev.llm_bedrock_test.service;

public interface LlmService {
    String chat(String sessionId, String userMessage);
}