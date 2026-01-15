package local.dev.llm_bedrock_test.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "llm.mode", havingValue = "stub", matchIfMissing = true)
public class StubLlmService implements LlmService {

    @Override
    public String chat(String sessionId, String userMessage) {
        return "[STUB] session=" + sessionId + " | You said: " + userMessage;
    }
}