package local.dev.llm_bedrock_test.lending;

import org.springframework.stereotype.Component;

@Component
public class LendingStateReducer {

    public void advanceStepIfPossible(ApplicationDraft d) {
        boolean progressed;
        int safety = 10;

        do {
            progressed = false;
            ApplicationDraft.Step before = d.getStep();

            switch (d.getStep()) {
                case START -> d.setStep(ApplicationDraft.Step.PERSONAL_DETAILS);

                case PERSONAL_DETAILS -> {
                    if (isPersonalComplete(d)) {
                        d.setStep(ApplicationDraft.Step.BUSINESS_DETAILS);
                    }
                }

                case BUSINESS_DETAILS -> {
                    if (isBusinessComplete(d)) {
                        d.setStep(ApplicationDraft.Step.FINANCIALS);
                    }
                }

                case FINANCIALS -> {
                    if (isFinancialsComplete(d)) {
                        d.setStep(ApplicationDraft.Step.REVIEW_SUBMIT);
                    }
                }

                case REVIEW_SUBMIT, SUBMITTED -> {
                    // no auto advance
                }
            }

            if (d.getStep() != before) progressed = true;
        } while (progressed && --safety > 0);
    }

    private boolean isPersonalComplete(ApplicationDraft d) {
        return notBlank(d.getFirstName()) && notBlank(d.getLastName()) && notBlank(d.getEmail());
    }

    private boolean isBusinessComplete(ApplicationDraft d) {
        return notBlank(d.getCompanyName()) && notBlank(d.getRegistrationId());
    }

    private boolean isFinancialsComplete(ApplicationDraft d) {
        return d.getRequestedAmount() != null && d.getTermMonths() != null && d.getMonthlyRevenue() != null;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
