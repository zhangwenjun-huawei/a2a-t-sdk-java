package net.openan.a2at.sdk.prompt.resources.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.hutool.core.convert.ConvertException;
import net.openan.a2at.sdk.core.exception.ResourceNotFoundException;
import net.openan.a2at.sdk.core.exception.SdkException;
import net.openan.a2at.sdk.resources.ClasspathPromptResourceLoader;
import org.junit.jupiter.api.Test;

class ClasspathPromptLoadersTest {

    private final ClasspathPromptResourceLoader resourceLoader = new ClasspathPromptResourceLoader();

    @Test
    void loadScenarioCatalogFailsWhenHutoolConvertsJsonObjectToRecordCatalog() {
        assertThrows(ConvertException.class, () -> new ClasspathPromptScenarioCatalogLoader(resourceLoader).load("en"));
    }

    @Test
    void loadTemplateReturnsExactMarkdownPayload() {
        String template = new ClasspathPromptTemplateLoader(resourceLoader).loadTemplate("handoff_planning", "en");

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
                template);
    }

    @Test
    void loadSlotSchemaFailsWhenHutoolConvertsJsonObjectToRecordSchema() {
        assertThrows(
                ConvertException.class,
                () -> new ClasspathPromptSlotSchemaLoader(resourceLoader).loadSlotSchema("handoff_planning", "en"));
    }

    @Test
    void missingClasspathResourceReportsResolvedResourcePath() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> new ClasspathPromptTemplateLoader(resourceLoader).loadTemplate("missing_scenario", "en"));

        assertEquals(
                "prompt_resources\\templates\\missing_scenario\\en\\template.md",
                exception.resourcePath());
    }

    @Test
    void invalidClasspathScenarioCatalogIsWrappedAsSdkException() {
        SdkException exception = assertThrows(
                SdkException.class,
                () -> new ClasspathPromptScenarioCatalogLoader(resourceLoader).load("broken"));

        assertEquals("Failed to parse scenario catalog for language: broken", exception.getMessage());
    }
}
