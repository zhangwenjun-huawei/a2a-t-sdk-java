package net.openan.a2at.sdk.prompt.analysis.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.openan.a2at.sdk.core.json.JsonValueParser;
import net.openan.a2at.sdk.llm.LLMClient;
import net.openan.a2at.sdk.llm.adapter.LLMAdapter;
import net.openan.a2at.sdk.llm.model.LLMResponse;
import net.openan.a2at.sdk.llm.model.LlmUsage;
import net.openan.a2at.sdk.llm.model.StructuredGenerationRequest;
import net.openan.a2at.sdk.llm.internal.assembly.DefaultLlmClientBuilder;
import net.openan.a2at.sdk.prompt.analysis.exception.ScenarioRecognitionException;
import net.openan.a2at.sdk.prompt.analysis.model.ScenarioRecognitionResult;
import net.openan.a2at.sdk.prompt.resources.model.ScenarioDefinition;
import org.junit.jupiter.api.Test;

class ScenarioRecognizerTest {

    @Test
    void recognizeBuildsStructuredMessagesAndReturnsMatchedScenario() throws IOException {
        RecordingAdapter adapter = new RecordingAdapter(
                "{\"matched\":true,\"scenario_code\":\"energy_saving\",\"error_message\":null}");
        LLMClient llmClient = buildClient(adapter);

        ScenarioRecognizer recognizer = new ScenarioRecognizer(llmClient);
        ScenarioRecognitionResult result = recognizer.recognize(
                "Please analyze site A energy usage.",
                List.of(new ScenarioDefinition(
                        "energy_saving", "Energy Saving", "Energy analysis", "Analyze site power")),
                "Identify the best matching scenario.",
                "Choose from the provided scenario list.");

        assertTrue(result.matched());
        assertEquals("energy_saving", result.scenarioCode());
        assertEquals(2, adapter.lastRequest().messages().size());
        assertEquals("system", adapter.lastRequest().messages().get(0).role());
        assertTrue(adapter.lastRequest().messages().get(1).content().contains("energy_saving"));
        assertTrue(adapter.lastRequest().jsonSchema().containsKey("required"));
    }

    @Test
    void recognizeRejectsMatchedPayloadWithoutScenarioCode() throws IOException {
        LLMClient llmClient = buildClient(new RecordingAdapter(
                "{\"matched\":true,\"scenario_code\":null,\"error_message\":null}"));

        ScenarioRecognizer recognizer = new ScenarioRecognizer(llmClient);

        assertThrows(
                ScenarioRecognitionException.class,
                () -> recognizer.recognize(
                        "Analyze site A energy usage.",
                        List.of(new ScenarioDefinition(
                                "energy_saving", "Energy Saving", "Energy analysis", "Analyze site power")),
                        "Identify the best matching scenario.",
                        "Choose from the provided scenario list."));
    }

    @Test
    void recognizeCanUseSharedJsonParserAbstraction() throws IOException {
        LLMClient llmClient = buildClient(new RecordingAdapter("ignored"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("matched", true);
        payload.put("scenario_code", "energy_saving");
        payload.put("error_message", null);
        RecordingJsonValueParser parser = new RecordingJsonValueParser(payload);

        ScenarioRecognizer recognizer = new ScenarioRecognizer(llmClient, parser);
        ScenarioRecognitionResult result = recognizer.recognize(
                "Please analyze site A energy usage.",
                List.of(new ScenarioDefinition(
                        "energy_saving", "Energy Saving", "Energy analysis", "Analyze site power")),
                "Identify the best matching scenario.",
                "Choose from the provided scenario list.");

        assertTrue(result.matched());
        assertEquals("energy_saving", result.scenarioCode());
        assertEquals("ignored", parser.lastPayload);
    }

    private static LLMClient buildClient(RecordingAdapter adapter) throws IOException {
        Path envFile = Files.createTempFile("a2at-scenario-recognizer", ".env");
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

        private StructuredGenerationRequest lastRequest;

        private RecordingAdapter(String payload) {
            this.payload = payload;
        }

        @Override
        public LLMResponse structured(StructuredGenerationRequest request) {
            this.lastRequest = request;
            return new LLMResponse(payload, "test-model", new LlmUsage(1, 1, 2), Map.of());
        }

        StructuredGenerationRequest lastRequest() {
            return lastRequest;
        }
    }

    private static final class RecordingJsonValueParser implements JsonValueParser {
        private final Map<String, Object> result;
        private String lastPayload;

        private RecordingJsonValueParser(Map<String, Object> result) {
            this.result = result;
        }

        @Override
        public Map<String, Object> parseObject(String payload) {
            this.lastPayload = payload;
            return result;
        }
    }
}
