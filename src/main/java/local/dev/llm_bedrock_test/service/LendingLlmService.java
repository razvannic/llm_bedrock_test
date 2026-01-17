package local.dev.llm_bedrock_test.service;


import local.dev.llm_bedrock_test.lending.agent.LendingAgentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "llm.mode", havingValue = "lending")
public class LendingLlmService implements LlmService {

    private final LendingAgentService agent;

    public LendingLlmService(LendingAgentService agent) {
        this.agent = agent;
    }

    @Override
    public String chat(String sessionId, String userMessage) {
        return agent.chat(sessionId, userMessage);
    }
}