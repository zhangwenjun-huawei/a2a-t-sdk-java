package net.openan.a2at.sdk.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.openan.a2at.sdk.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

class ClasspathPromptResourceLoaderTest {

    private final ClasspathPromptResourceLoader loader = new ClasspathPromptResourceLoader();

    @Test
    void loadsTextResourceUsingPromptResourceKey() {
        PromptResourceKey key = PromptResourceKey.prompt("slot_extraction", "en-US", "system.md");

        String text = loader.loadText(key);

        assertEquals("system prompt", text.trim());
    }

    @Test
    void rejectsResourceTraversalSegments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PromptResourceKey("prompts", "../escape", "en-US", "system.md"));
    }

    @Test
    void raisesTypedErrorWhenResourceIsMissing() {
        PromptResourceKey key = PromptResourceKey.template("missing_scenario", "en-US", "template.md");

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class, () -> loader.loadText(key));

        assertEquals("prompt_resources/templates/missing_scenario/en-US/template.md", error.resourcePath().replace('\\', '/'));
    }

    @Test
    void loadsPackagedScenarioCatalogForZhCn() {
        String text = loader.loadText(new PromptResourceKey("scenarios", "catalog", "zh-CN", "scenarios.json"));

        org.junit.jupiter.api.Assertions.assertTrue(text.contains("subscribe_incident"));
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("energy_saving"));
    }

    @Test
    void loadsPackagedSubscribeIncidentSlotSchemaWithSemanticHint() {
        String text = loader.loadText(new PromptResourceKey("slots", "subscribe_incident", "zh-CN", "slot.json"));

        assertTrue(text.contains("\"required\": []"));
        assertTrue(text.contains("x-a2at-value-constraint"));
        assertTrue(text.contains("critical"));
        assertTrue(text.contains("high"));
        assertTrue(text.contains("medium"));
        assertTrue(text.contains("low"));
    }

    @Test
    void loadsPackagedFaultDiagnosisScenarioCatalogWithLatestShortExample() {
        String text = loader.loadText(new PromptResourceKey("scenarios", "catalog", "zh-CN", "scenarios.json"));

        assertTrue(text.contains("\"scenario_code\": \"fault_diagnosis\""));
        assertTrue(text.contains("1856365516_2839324485_2130908106_4130674041"));
        assertFalse(text.contains("task_request_id"));
    }

    @Test
    void loadsPackagedFaultDiagnosisSlotSchemaWithLatestConciseDescriptions() {
        String text = loader.loadText(new PromptResourceKey("slots", "fault_diagnosis", "zh-CN", "slot.json"));

        assertTrue(text.contains("\"type\": \"string\""));
        assertTrue(text.contains("\"required\": ["));
        assertTrue(text.contains("DataPart"));
        assertTrue(text.contains("TextPart"));
        assertFalse(text.contains("task_request_id"));
    }

    @Test
    void loadsPackagedFaultDiagnosisTemplateWithoutLegacyLongExamples() {
        String text = loader.loadText(new PromptResourceKey("templates", "fault_diagnosis", "zh-CN", "template.md"));

        assertTrue(text.contains("Task Type"));
        assertTrue(text.contains("Task Target"));
        assertTrue(text.contains("DataPart"));
        assertTrue(text.contains("TextPart"));
        assertFalse(text.contains("Full request example"));
        assertFalse(text.contains("Full response example"));
    }
}
