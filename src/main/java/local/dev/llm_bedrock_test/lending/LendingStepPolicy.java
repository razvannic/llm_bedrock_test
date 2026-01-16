package local.dev.llm_bedrock_test.lending;

import local.dev.llm_bedrock_test.lending.tools.LendingToolRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class LendingStepPolicy {

    public boolean isToolAllowed(ApplicationDraft.Step step, String toolName) {
        return allowedToolsForStep(step).contains(toolName);
    }

    public Set<String> allowedToolsForStep(ApplicationDraft.Step step) {
        return switch (step) {
            case START, PERSONAL_DETAILS -> Set.of(LendingToolRegistry.GET_STATUS, LendingToolRegistry.UPDATE_PERSONAL);
            case BUSINESS_DETAILS -> Set.of(LendingToolRegistry.GET_STATUS, LendingToolRegistry.UPDATE_BUSINESS);
            case FINANCIALS -> Set.of(LendingToolRegistry.GET_STATUS, LendingToolRegistry.UPDATE_FINANCIALS);
            case REVIEW_SUBMIT -> Set.of(LendingToolRegistry.GET_STATUS, LendingToolRegistry.SUBMIT);
            case SUBMITTED -> Set.of(LendingToolRegistry.GET_STATUS);
        };
    }
}
