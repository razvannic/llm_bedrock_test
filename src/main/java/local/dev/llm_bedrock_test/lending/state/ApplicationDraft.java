package local.dev.llm_bedrock_test.lending.state;

import java.math.BigDecimal;
import java.util.Objects;

public class ApplicationDraft {

    public enum Step {
        START,
        PERSONAL_DETAILS,
        BUSINESS_DETAILS,
        FINANCIALS,
        REVIEW_SUBMIT,
        SUBMITTED
    }

    private Step step = Step.START;

    // Personal
    private String firstName;
    private String lastName;
    private String email;

    // Business
    private String companyName;
    private String registrationId;

    // Financials
    private BigDecimal requestedAmount;
    private Integer termMonths;
    private BigDecimal monthlyRevenue;

    public Step getStep() { return step; }
    public void setStep(Step step) { this.step = Objects.requireNonNull(step); }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getRegistrationId() { return registrationId; }
    public void setRegistrationId(String registrationId) { this.registrationId = registrationId; }

    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }

    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }

    public BigDecimal getMonthlyRevenue() { return monthlyRevenue; }
    public void setMonthlyRevenue(BigDecimal monthlyRevenue) { this.monthlyRevenue = monthlyRevenue; }
}
