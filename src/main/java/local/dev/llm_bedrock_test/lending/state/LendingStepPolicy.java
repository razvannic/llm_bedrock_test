package local.dev.llm_bedrock_test.lending.state;

import local.dev.llm_bedrock_test.lending.tools.ToolNames;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class LendingStepPolicy {

    public boolean isToolAllowed(ApplicationDraft.Step step, String toolName) {
        return allowedToolsForStep(step).contains(toolName);
    }

    public Set<String> allowedToolsForStep(ApplicationDraft.Step step) {
        return switch (step) {
            case START, PERSONAL_DETAILS -> Set.of(ToolNames.GET_STATUS, ToolNames.UPDATE_PERSONAL);
            case BUSINESS_DETAILS -> Set.of(ToolNames.GET_STATUS, ToolNames.UPDATE_BUSINESS);
            case FINANCIALS -> Set.of(ToolNames.GET_STATUS, ToolNames.UPDATE_FINANCIALS);
            case REVIEW_SUBMIT -> Set.of(ToolNames.GET_STATUS, ToolNames.SUBMIT);
            case SUBMITTED -> Set.of(ToolNames.GET_STATUS);
        };
    }
}
