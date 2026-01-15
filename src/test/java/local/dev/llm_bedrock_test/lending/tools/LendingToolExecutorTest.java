package local.dev.llm_bedrock_test.lending.tools;

import local.dev.llm_bedrock_test.lending.ApplicationDraft;
import local.dev.llm_bedrock_test.lending.DraftStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.document.Document;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/*
    Test LendingToolExecutor step transitions and validation.
    This gives you strong confidence quickly because it’s not probabilistic.
 */

class LendingToolExecutorTest {

    private DraftStore store;
    private LendingToolExecutor exec;

    @BeforeEach
    void setup() {
        store = new DraftStore();
        exec = new LendingToolExecutor(store);
    }

    @Test
    void updatePersonal_whenAllPersonalProvided_movesToBusinessDetails() {
        String sessionId = "s1";

        Document input = Document.fromMap(Map.of(
                "sessionId", Document.fromString(sessionId),
                "firstName", Document.fromString("Razvan"),
                "lastName", Document.fromString("Nicolae"),
                "email", Document.fromString("razvan@test.com")
        ));

        Document out = exec.execute(LendingToolRegistry.UPDATE_PERSONAL, input);

        ApplicationDraft draft = store.getOrCreate(sessionId);
        assertEquals(ApplicationDraft.Step.BUSINESS_DETAILS, draft.getStep());

        assertEquals("OK", out.asMap().get("status").asString());
        assertEquals("BUSINESS_DETAILS", out.asMap().get("step").asString());
    }

    @Test
    void updateBusiness_whenComplete_movesToFinancials() {
        String sessionId = "s1";
        ApplicationDraft d = store.getOrCreate(sessionId);
        d.setStep(ApplicationDraft.Step.BUSINESS_DETAILS);

        Document input = Document.fromMap(Map.of(
                "sessionId", Document.fromString(sessionId),
                "companyName", Document.fromString("ABC SRL"),
                "registrationId", Document.fromString("RO123456")
        ));

        exec.execute(LendingToolRegistry.UPDATE_BUSINESS, input);

        assertEquals(ApplicationDraft.Step.FINANCIALS, d.getStep());
    }

    @Test
    void submit_whenMissingFields_returnsIncomplete() {
        String sessionId = "s1";
        ApplicationDraft d = store.getOrCreate(sessionId);
        d.setStep(ApplicationDraft.Step.REVIEW_SUBMIT);

        Document input = Document.fromMap(Map.of(
                "sessionId", Document.fromString(sessionId)
        ));

        Document out = exec.execute(LendingToolRegistry.SUBMIT, input);

        assertEquals("INCOMPLETE", out.asMap().get("status").asString());
        assertNotNull(out.asMap().get("missingFields"));
        assertEquals(ApplicationDraft.Step.REVIEW_SUBMIT, d.getStep()); // not submitted
    }

    @Test
    void happyPath_fullSequence_endsSubmitted() {
        String sessionId = "s1";

        exec.execute(LendingToolRegistry.UPDATE_PERSONAL, Document.fromMap(Map.of(
                "sessionId", Document.fromString(sessionId),
                "firstName", Document.fromString("Razvan"),
                "lastName", Document.fromString("Nicolae"),
                "email", Document.fromString("razvan@test.com")
        )));
        assertEquals(ApplicationDraft.Step.BUSINESS_DETAILS, store.getOrCreate(sessionId).getStep());

        exec.execute(LendingToolRegistry.UPDATE_BUSINESS, Document.fromMap(Map.of(
                "sessionId", Document.fromString(sessionId),
                "companyName", Document.fromString("ABC SRL"),
                "registrationId", Document.fromString("RO123456")
        )));
        assertEquals(ApplicationDraft.Step.FINANCIALS, store.getOrCreate(sessionId).getStep());

        exec.execute(LendingToolRegistry.UPDATE_FINANCIALS, Document.fromMap(Map.of(
                "sessionId", Document.fromString(sessionId),
                "requestedAmount", Document.fromNumber(50000),
                "termMonths", Document.fromNumber(24),
                "monthlyRevenue", Document.fromNumber(20000)
        )));
        assertEquals(ApplicationDraft.Step.REVIEW_SUBMIT, store.getOrCreate(sessionId).getStep());

        Document submitOut = exec.execute(LendingToolRegistry.SUBMIT, Document.fromMap(Map.of(
                "sessionId", Document.fromString(sessionId)
        )));
        assertEquals("SUBMITTED", submitOut.asMap().get("status").asString());
        assertEquals(ApplicationDraft.Step.SUBMITTED, store.getOrCreate(sessionId).getStep());
    }

    @Test
    void updateFinancials_whenInPersonalDetails_returnsStepViolation() {
        String sessionId = "s1";
        ApplicationDraft d = store.getOrCreate(sessionId);
        d.setStep(ApplicationDraft.Step.PERSONAL_DETAILS);

        Document input = Document.fromMap(Map.of(
                "sessionId", Document.fromString(sessionId),
                "requestedAmount", Document.fromNumber(50000),
                "termMonths", Document.fromNumber(24),
                "monthlyRevenue", Document.fromNumber(20000)
        ));

        Document out = exec.execute(LendingToolRegistry.UPDATE_FINANCIALS, input);

        assertEquals("ERROR", out.asMap().get("status").asString());
        assertEquals("STEP_VIOLATION", out.asMap().get("code").asString());
        assertEquals("PERSONAL_DETAILS", out.asMap().get("currentStep").asString());
    }

}
