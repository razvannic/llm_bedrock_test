package local.dev.llm_bedrock_test.lending.state;

import org.springframework.stereotype.Component;

@Component
public class LendingStateReducer {

    public void advanceStepIfPossible(ApplicationDraft d) {
        if (d == null) return;

        ApplicationDraft.Step current = d.getStep();
        ApplicationDraft.Step next = computeNextStep(current, d);

        // IMPORTANT: only move forward
        if (next.ordinal() > current.ordinal()) {
            d.setStep(next);
        }
    }

    private ApplicationDraft.Step computeNextStep(ApplicationDraft.Step current, ApplicationDraft d) {
        // if already submitted, stay
        if (current == ApplicationDraft.Step.SUBMITTED) return ApplicationDraft.Step.SUBMITTED;

        // If you're in START, first interaction brings you to PERSONAL_DETAILS.
        if (current == ApplicationDraft.Step.START) return ApplicationDraft.Step.PERSONAL_DETAILS;

        // only advance if current step is complete
        if (current == ApplicationDraft.Step.PERSONAL_DETAILS && isPersonalComplete(d)) {
            return ApplicationDraft.Step.BUSINESS_DETAILS;
        }
        if (current == ApplicationDraft.Step.BUSINESS_DETAILS && isBusinessComplete(d)) {
            return ApplicationDraft.Step.FINANCIALS;
        }
        if (current == ApplicationDraft.Step.FINANCIALS && isFinancialsComplete(d)) {
            return ApplicationDraft.Step.REVIEW_SUBMIT;
        }
        if (current == ApplicationDraft.Step.REVIEW_SUBMIT && isAllComplete(d)) {
            return ApplicationDraft.Step.SUBMITTED;
        }

        // default: stay where you are
        return current;
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

    private boolean isAllComplete(ApplicationDraft d) {
        return isPersonalComplete(d) && isBusinessComplete(d) && isFinancialsComplete(d);
    }
}
