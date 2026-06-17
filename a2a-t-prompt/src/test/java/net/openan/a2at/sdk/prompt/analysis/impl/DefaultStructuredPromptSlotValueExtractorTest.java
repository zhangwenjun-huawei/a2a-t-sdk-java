package net.openan.a2at.sdk.prompt.analysis.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.openan.a2at.sdk.llm.LLMClient;
import net.openan.a2at.sdk.llm.adapter.LLMAdapter;
import net.openan.a2at.sdk.llm.internal.assembly.DefaultLlmClientBuilder;
import net.openan.a2at.sdk.llm.model.LLMResponse;
import net.openan.a2at.sdk.llm.model.LlmUsage;
import net.openan.a2at.sdk.llm.model.StructuredGenerationRequest;
import net.openan.a2at.sdk.prompt.analysis.model.StructuredSlotExtractionResult;
import net.openan.a2at.sdk.prompt.resources.model.PromptSlotDefinition;
import net.openan.a2at.sdk.prompt.resources.model.PromptSlotSchema;
import org.junit.jupiter.api.Test;

class DefaultStructuredPromptSlotValueExtractorTest {

    @Test
    void extractSlotsParsesFormattedStructuredJsonPayload() throws IOException {
        LLMClient llmClient = buildClient(new RecordingAdapter("""
                {
                  "slots": {
                    "site": "Site A",
                    "additional_notes": null,
                    "limit": "5",
                    "severity": "high"
                  },
                  "slot_errors": []
                }
                """));
        DefaultStructuredPromptSlotValueExtractor extractor = new DefaultStructuredPromptSlotValueExtractor(
                llmClient,
                (scenarioCode, language) -> new PromptSlotSchema(
                        scenarioCode,
                        List.of(
                                new PromptSlotDefinition("site", true, "string", null, null, null, null, null),
                                new PromptSlotDefinition(
                                        "additional_notes", false, "string", null, null, null, null, null),
                                new PromptSlotDefinition("limit", false, "integer", null, 1.0d, 10.0d, null, null),
                                new PromptSlotDefinition(
                                        "severity",
                                        false,
                                        "string",
                                        null,
                                        null,
                                        null,
                                        List.of("low", "medium", "high"),
                                        null))),
                "Extract slots from the input.",
                "Return slots as JSON.");

        StructuredSlotExtractionResult result =
                extractor.extractSlots("Analyze Site A with critical severity.", "energy_saving", "en-US");

        assertEquals(
                Map.of(
                        "site", "Site A",
                        "additional_notes", "",
                        "limit", "5",
                        "severity", "high"),
                result.slots());
        assertEquals(List.of(), result.slotErrors());
    }

    @Test
    void extractSlotsPreservesSlotTextContainingClosingBrace() throws IOException {
        LLMClient llmClient = buildClient(new RecordingAdapter("""
                {
                  "slots": {
                    "site": "Site A",
                    "additional_notes": "Need } fallback"
                  },
                  "slot_errors": []
                }
                """));
        DefaultStructuredPromptSlotValueExtractor extractor = new DefaultStructuredPromptSlotValueExtractor(
                llmClient,
                (scenarioCode, language) -> new PromptSlotSchema(
                        scenarioCode,
                        List.of(
                                new PromptSlotDefinition("site", true, "string", null, null, null, null, null),
                                new PromptSlotDefinition(
                                        "additional_notes", false, "string", null, null, null, null, null))),
                "Extract slots from the input.",
                "Return slots as JSON.");

        StructuredSlotExtractionResult result =
                extractor.extractSlots("Analyze Site A with fallback note.", "energy_saving", "en-US");

        assertEquals(Map.of("site", "Site A", "additional_notes", "Need } fallback"), result.slots());
        assertEquals(List.of(), result.slotErrors());
    }

    private static LLMClient buildClient(RecordingAdapter adapter) throws IOException {
        Path envFile = Files.createTempFile("a2at-prompt-slot-extractor", ".env");
        Files.writeString(
                envFile,
                """
                A2AT_LLM_PROVIDER=openai_compatible
                A2AT_LLM_MODEL=test-model
                A2AT_LLM_API_KEY=test-key
                """);
        return DefaultLlmClientBuilder.builder().adapter(adapter).build(envFile);
    }

    private static final class RecordingAdapter implements LLMAdapter {

        private final String payload;

        private RecordingAdapter(String payload) {
            this.payload = payload;
        }

        @Override
        public LLMResponse structured(StructuredGenerationRequest request) {
            return new LLMResponse(payload, "test-model", new LlmUsage(1, 1, 2), Map.of());
        }
    }
}
