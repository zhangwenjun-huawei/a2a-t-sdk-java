package net.openan.a2at.sdk.prompt.resources.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.core.convert.ConvertException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.openan.a2at.sdk.core.exception.ResourceNotFoundException;
import net.openan.a2at.sdk.core.model.PromptRuntimeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PromptResourceAccessTest {

    @TempDir
    Path promptRootDir;

    @Test
    void createClasspathAccessLoadsBundledPromptResourcesAndRejectsLocalRootAccess() {
        PromptResourceAccess access = PromptResourceAccess.create(new PromptRuntimeConfig("classpath", null));

        assertTrue(access.classpath());
        assertThrows(UnsupportedOperationException.class, access::localRootDir);
        assertEquals(
                """
                You are the analysis prompt used by the a2a-t SDK resource loading tests.

                Analyze the incoming task request and return a compact JSON object with:

                - `scenario_code`: the best matching scenario from the catalog
                - `confidence`: a number between 0 and 1
                - `missing_slots`: required slots that were not present in the user input
                - `evidence`: short text spans that justify the classification

                Prefer deterministic wording so resource tests can compare the loaded payload exactly.
                """,
                access.loadPrompt("analysis", "en", "system.md"));
        assertEquals(
                """
                # Cross-Team Handoff Plan

                ## Context

                Scenario: {{scenario_code}}
                Primary owner: {{owner}}
                Target system: {{target_system}}
                Deadline: {{deadline}}

                ## Required Output

                1. Summarize the current state in two concise paragraphs.
                2. List the owners for engineering, security, product, and operations.
                3. Identify blocking dependencies and the expected unblock date.
                4. Provide a rollback path that can be executed by the on-call team.

                ## Acceptance Criteria

                - Every required slot is represented in the plan.
                - Risks are grouped by severity and owner.
                - Follow-up tasks include a due date and escalation path.
                """,
                access.templateLoader().loadTemplate("handoff_planning", "en"));
        assertThrows(ConvertException.class, () -> access.loadScenarios("en"));
    }

    @Test
    void createLocalFileAccessLoadsPromptsTemplatesScenariosAndSlotsFromSameRoot() throws IOException {
        write(promptRootDir.resolve("prompts").resolve("analysis").resolve("en").resolve("system.md"), "Local system prompt.");
        write(
                promptRootDir
                        .resolve("templates")
                        .resolve("incident_triage")
                        .resolve("en")
                        .resolve("template.md"),
                "Local template for {{service}}.");
        write(
                promptRootDir.resolve("scenarios").resolve("en").resolve("scenarios.json"),
                """
                {
                  "scenarios": [
                    {
                      "scenario_code": "incident_triage",
                      "scenario_name": "Incident Triage",
                      "description": "Classify and route a production incident.",
                      "example": "Investigate elevated API latency."
                    }
                  ]
                }
                """);
        write(
                promptRootDir.resolve("slots").resolve("incident_triage").resolve("en").resolve("slot.json"),
                """
                {
                  "required": ["service"],
                  "properties": {
                    "service": {"type": "string", "description": "Affected service name"}
                  }
                }
                """);

        PromptResourceAccess access =
                PromptResourceAccess.create(new PromptRuntimeConfig("local_file", promptRootDir.toString()));

        assertFalse(access.classpath());
        assertEquals(promptRootDir, access.localRootDir());
        assertThrows(UnsupportedOperationException.class, access::classpathResourceLoader);
        assertEquals("Local system prompt.", access.loadPrompt("analysis", "en", "system.md"));
        assertEquals("Local template for {{service}}.", access.templateLoader().loadTemplate("incident_triage", "en"));
        assertThrows(ConvertException.class, () -> access.loadScenarios("en"));
        assertThrows(ConvertException.class, () -> access.slotSchemaLoader().loadSlotSchema("incident_triage", "en"));
    }

    @Test
    void localPromptResourceNotFoundIncludesResolvedPromptPath() {
        PromptResourceAccess access =
                PromptResourceAccess.create(new PromptRuntimeConfig("local_file", promptRootDir.toString()));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> access.loadPrompt("analysis", "en", "missing.md"));

        assertEquals(
                promptRootDir.resolve("prompts").resolve("analysis").resolve("en").resolve("missing.md").toString(),
                exception.resourcePath());
    }

    @Test
    void unsupportedSourceTypeFailsFast() {
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> PromptResourceAccess.create(new PromptRuntimeConfig("database", promptRootDir.toString())));

        assertEquals("Unsupported prompt source type: database", exception.getMessage());
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
