package local.dev.llm_bedrock_test.lending;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class DraftStore {

    private final ConcurrentHashMap<String, ApplicationDraft> drafts = new ConcurrentHashMap<>();

    public ApplicationDraft getOrCreate(String sessionId) {
        return drafts.computeIfAbsent(sessionId, id -> new ApplicationDraft());
    }
}