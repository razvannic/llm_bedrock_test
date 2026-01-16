package local.dev.llm_bedrock_test.lending.tools;

import local.dev.llm_bedrock_test.lending.ApplicationDraft;
import local.dev.llm_bedrock_test.lending.DraftStore;
import local.dev.llm_bedrock_test.lending.LendingStepPolicy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.document.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class LendingToolExecutor {

    private final DraftStore store;
    private final LendingStepPolicy stepPolicy;

    public LendingToolExecutor(DraftStore store, LendingStepPolicy stepPolicy) {
        this.store = store;
        this.stepPolicy = stepPolicy;
    }

    public Document execute(String toolName, Document input) {
        Map<String, Document> m = input.asMap();
        String sessionId = string(m, "sessionId");

        if (sessionId == null || sessionId.isBlank()) {
            return error("INVALID_INPUT", "sessionId is required", null, null);
        }

        ApplicationDraft draft = store.getOrCreate(sessionId);

        // STRICT STEP GUARD - “reference” implementation for local mode - can be removed if we remove local mode completely
        if (!stepPolicy.isToolAllowed(draft.getStep(), toolName)) {
            return error(
                    "STEP_VIOLATION",
                    "Tool '" + toolName + "' is not allowed in step " + draft.getStep(),
                    draft.getStep().name(),
                    stepPolicy.allowedToolsForStep(draft.getStep())
            );
        }

        return switch (toolName) {
            case LendingToolRegistry.GET_STATUS -> getStatus(draft);
            case LendingToolRegistry.UPDATE_PERSONAL -> updatePersonal(draft, m);
            case LendingToolRegistry.UPDATE_BUSINESS -> updateBusiness(draft, m);
            case LendingToolRegistry.UPDATE_FINANCIALS -> updateFinancials(draft, m);
            case LendingToolRegistry.SUBMIT -> submit(draft);
            default -> Document.mapBuilder()
                    .putString("status", "ERROR")
                    .putString("message", "Unknown tool: " + toolName)
                    .build();
        };
    }

    private Document error(String code, String message, String currentStep, Set<String> allowedTools) {
        Document.MapBuilder b = Document.mapBuilder()
                .putString("status", "ERROR")
                .putString("code", code)
                .putString("message", message);

        if (currentStep != null) b.putString("currentStep", currentStep);
        if (allowedTools != null) {
            b.putList("allowedTools", allowedTools.stream().map(Document::fromString).toList());
        }
        return b.build();
    }

    private Document getStatus(ApplicationDraft draft) {
        return Document.mapBuilder()
                .putString("status", "OK")
                .putString("step", draft.getStep().name())
                .putList("missingFields", missingFields(draft).stream().map(Document::fromString).toList())
                .build();
    }

    private Document updatePersonal(ApplicationDraft draft, Map<String, Document> m) {
        if (m.containsKey("firstName")) draft.setFirstName(m.get("firstName").asString());
        if (m.containsKey("lastName")) draft.setLastName(m.get("lastName").asString());
        if (m.containsKey("email")) draft.setEmail(m.get("email").asString());

        if (draft.getStep() == ApplicationDraft.Step.START) {
            draft.setStep(ApplicationDraft.Step.PERSONAL_DETAILS);
        }
        // If personal is complete, move forward
        if (isPersonalComplete(draft) && draft.getStep() == ApplicationDraft.Step.PERSONAL_DETAILS) {
            draft.setStep(ApplicationDraft.Step.BUSINESS_DETAILS);
        }

        return getStatus(draft);
    }

    private Document updateBusiness(ApplicationDraft draft, Map<String, Document> m) {
        if (m.containsKey("companyName")) draft.setCompanyName(m.get("companyName").asString());
        if (m.containsKey("registrationId")) draft.setRegistrationId(m.get("registrationId").asString());

        if (draft.getStep().ordinal() < ApplicationDraft.Step.BUSINESS_DETAILS.ordinal()) {
            draft.setStep(ApplicationDraft.Step.BUSINESS_DETAILS);
        }
        if (isBusinessComplete(draft) && draft.getStep() == ApplicationDraft.Step.BUSINESS_DETAILS) {
            draft.setStep(ApplicationDraft.Step.FINANCIALS);
        }

        return getStatus(draft);
    }

    private Document updateFinancials(ApplicationDraft draft, Map<String, Document> m) {
        if (m.containsKey("requestedAmount")) draft.setRequestedAmount(new BigDecimal(m.get("requestedAmount").asNumber().toString()));
        if (m.containsKey("termMonths")) draft.setTermMonths((int) m.get("termMonths").asNumber().longValue());
        if (m.containsKey("monthlyRevenue")) draft.setMonthlyRevenue(new BigDecimal(m.get("monthlyRevenue").asNumber().toString()));

        if (draft.getStep().ordinal() < ApplicationDraft.Step.FINANCIALS.ordinal()) {
            draft.setStep(ApplicationDraft.Step.FINANCIALS);
        }
        if (isFinancialsComplete(draft) && draft.getStep() == ApplicationDraft.Step.FINANCIALS) {
            draft.setStep(ApplicationDraft.Step.REVIEW_SUBMIT);
        }

        return getStatus(draft);
    }

    private Document submit(ApplicationDraft draft) {
        List<String> missing = missingFields(draft);
        if (!missing.isEmpty()) {
            return Document.mapBuilder()
                    .putString("status", "INCOMPLETE")
                    .putString("step", draft.getStep().name())
                    .putList("missingFields", missing.stream().map(Document::fromString).toList())
                    .build();
        }
        draft.setStep(ApplicationDraft.Step.SUBMITTED);
        return Document.mapBuilder()
                .putString("status", "SUBMITTED")
                .putString("step", draft.getStep().name())
                .build();
    }

    private List<String> missingFields(ApplicationDraft d) {
        List<String> missing = new ArrayList<>();
        if (d.getFirstName() == null) missing.add("firstName");
        if (d.getLastName() == null) missing.add("lastName");
        if (d.getEmail() == null) missing.add("email");
        if (d.getCompanyName() == null) missing.add("companyName");
        if (d.getRegistrationId() == null) missing.add("registrationId");
        if (d.getRequestedAmount() == null) missing.add("requestedAmount");
        if (d.getTermMonths() == null) missing.add("termMonths");
        if (d.getMonthlyRevenue() == null) missing.add("monthlyRevenue");
        return missing;
    }

    private boolean isPersonalComplete(ApplicationDraft d) {
        return d.getFirstName() != null && d.getLastName() != null && d.getEmail() != null;
    }

    private boolean isBusinessComplete(ApplicationDraft d) {
        return d.getCompanyName() != null && d.getRegistrationId() != null;
    }

    private boolean isFinancialsComplete(ApplicationDraft d) {
        return d.getRequestedAmount() != null && d.getTermMonths() != null && d.getMonthlyRevenue() != null;
    }

    private String string(Map<String, Document> m, String k) {
        Document v = m.get(k);
        return v == null ? null : v.asString();
    }
}
