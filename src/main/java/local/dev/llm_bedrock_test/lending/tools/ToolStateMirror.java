package local.dev.llm_bedrock_test.lending.tools;

import local.dev.llm_bedrock_test.lending.state.ApplicationDraft;
import local.dev.llm_bedrock_test.lending.state.DraftStore;
import local.dev.llm_bedrock_test.lending.state.LendingStateReducer;
import local.dev.llm_bedrock_test.lending.state.LendingStepPolicy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.document.Document;

import java.math.BigDecimal;
import java.util.Map;

import static local.dev.llm_bedrock_test.lending.tools.ToolNames.*;

@Component
public class ToolStateMirror {

    private final DraftStore store;
    private final LendingStepPolicy stepPolicy;
    private final LendingStateReducer reducer;

    public ToolStateMirror(DraftStore store, LendingStepPolicy stepPolicy, LendingStateReducer reducer) {
        this.store = store;
        this.stepPolicy = stepPolicy;
        this.reducer = reducer;
    }

    public void apply(String toolName, Document input) {
        if (input == null || !input.isMap()) return;

        Map<String, Document> m = input.asMap();
        String sessionId = getString(m, "sessionId");
        if (sessionId == null) return;

        ApplicationDraft d = store.getOrCreate(sessionId);

        boolean allowed = stepPolicy.isToolAllowed(d.getStep(), toolName);
        System.out.println("MIRROR tool=" + toolName + " allowed=" + allowed + " stepBefore=" + d.getStep());
        if (GET_STATUS.equals(toolName)) return;

        // Always mirror fields (so we never lose info)
        switch (toolName) {
            case UPDATE_PERSONAL -> {
                setIfPresent(d, m, "firstName", d::setFirstName);
                setIfPresent(d, m, "lastName", d::setLastName);
                setIfPresent(d, m, "email", d::setEmail);

                // Explicit transition (matches old local executor behavior)
                if (d.getStep() == ApplicationDraft.Step.START) {
                    d.setStep(ApplicationDraft.Step.PERSONAL_DETAILS);
                }
                if (isPersonalComplete(d) && d.getStep() == ApplicationDraft.Step.PERSONAL_DETAILS) {
                    d.setStep(ApplicationDraft.Step.BUSINESS_DETAILS);
                }
            }
            case UPDATE_BUSINESS -> {
                setIfPresent(d, m, "companyName", d::setCompanyName);
                setIfPresent(d, m, "registrationId", d::setRegistrationId);
            }
            case UPDATE_FINANCIALS -> {
                if (m.get("requestedAmount") != null && m.get("requestedAmount").isNumber()) {
                    d.setRequestedAmount(new BigDecimal(m.get("requestedAmount").asNumber().toString()));
                }
                if (m.get("termMonths") != null && m.get("termMonths").isNumber()) {
                    d.setTermMonths((int) m.get("termMonths").asNumber().longValue());
                }
                if (m.get("monthlyRevenue") != null && m.get("monthlyRevenue").isNumber()) {
                    d.setMonthlyRevenue(new BigDecimal(m.get("monthlyRevenue").asNumber().toString()));
                }
            }
            case SUBMIT -> {
                if (allowed) d.setStep(ApplicationDraft.Step.SUBMITTED);
            }
            default -> { /* no-op */ }
        }

        // Only step-transition when tool is allowed in the current step
        if (allowed) {
            reducer.advanceStepIfPossible(d);
        }
        System.out.println("MIRROR AFTER tool=" + toolName + " step=" + d.getStep()
                + " personal=" + d.getFirstName() + " " + d.getLastName() + " " + d.getEmail());

    }

    private boolean isPersonalComplete(ApplicationDraft d) {
        return d.getFirstName() != null && d.getLastName() != null && d.getEmail() != null;
    }

    private void setIfPresent(ApplicationDraft d, Map<String, Document> m, String key, java.util.function.Consumer<String> setter) {
        String v = getString(m, key);
        if (v != null) setter.accept(v);
    }

    private String getString(Map<String, Document> m, String key) {
        Document v = m.get(key);
        return (v != null && v.isString()) ? v.asString() : null;
    }
}