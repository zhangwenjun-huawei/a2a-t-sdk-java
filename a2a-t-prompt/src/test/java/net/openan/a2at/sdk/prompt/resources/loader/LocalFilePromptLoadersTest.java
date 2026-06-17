package net.openan.a2at.sdk.prompt.resources.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.hutool.core.convert.ConvertException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.openan.a2at.sdk.core.exception.ResourceNotFoundException;
import net.openan.a2at.sdk.core.exception.SdkException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFilePromptLoadersTest {

    @TempDir
    Path promptRootDir;

    @Test
    void loadScenarioCatalogFailsWhenHutoolConvertsJsonObjectToRecordCatalog() throws IOException {
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
                    },
                    {
                      "scenario_code": "release_review",
                      "scenario_name": "Release Review",
                      "description": "Review a release candidate before rollout.",
                      "example": "Check the payment service release."
                    }
                  ]
                }
                """);

        assertThrows(ConvertException.class, () -> new LocalFilePromptScenarioCatalogLoader(promptRootDir).load("en"));
    }

    @Test
    void loadScenarioCatalogTreatsMissingScenariosArrayAsEmptyCatalog() throws IOException {
        write(promptRootDir.resolve("scenarios").resolve("en").resolve("scenarios.json"), "{}");

        assertEquals(List.of(), new LocalFilePromptScenarioCatalogLoader(promptRootDir).load("en"));
    }

    @Test
    void loadTemplateReadsExactLocalMarkdownText() throws IOException {
        write(
                promptRootDir
                        .resolve("templates")
                        .resolve("incident_triage")
                        .resolve("en")
                        .resolve("template.md"),
                """
                # Incident Triage

                Severity: {{severity}}
                Summary: {{summary}}
                """);

        String template = new LocalFilePromptTemplateLoader(promptRootDir).loadTemplate("incident_triage", "en");

        assertEquals(
                """
                # Incident Triage

                Severity: {{severity}}
                Summary: {{summary}}
                """,
                template);
    }

    @Test
    void loadSlotSchemaFailsWhenHutoolConvertsJsonObjectToRecordSchema() throws IOException {
        write(
                promptRootDir.resolve("slots").resolve("incident_triage").resolve("en").resolve("slot.json"),
                """
                {
                  "required": ["severity", "service"],
                  "properties": {
                    "service": {
                      "type": "string",
                      "pattern": "^[a-z0-9-]+$",
                      "description": "Affected service name"
                    },
                    "severity": {
                      "type": "integer",
                      "minimum": 1,
                      "maximum": 5,
                      "enum": ["1", "2", "3", "4", "5"],
                      "x-a2at-value-constraint": "Severity from 1 to 5"
                    },
                    "note": null
                  }
                }
                """);

        assertThrows(
                ConvertException.class,
                () -> new LocalFilePromptSlotSchemaLoader(promptRootDir).loadSlotSchema("incident_triage", "en"));
    }

    @Test
    void missingTemplateIncludesResolvedLocalPathInResourceNotFoundException() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> new LocalFilePromptTemplateLoader(promptRootDir).loadTemplate("incident_triage", "en"));

        assertEquals(
                promptRootDir
                        .resolve("templates")
                        .resolve("incident_triage")
                        .resolve("en")
                        .resolve("template.md")
                        .toString(),
                exception.resourcePath());
    }

    @Test
    void malformedLocalSlotSchemaIsWrappedAsSdkException() throws IOException {
        write(
                promptRootDir.resolve("slots").resolve("incident_triage").resolve("en").resolve("slot.json"),
                "{ \"required\": [\"severity\"], \"properties\": ");

        SdkException exception = assertThrows(
                SdkException.class,
                () -> new LocalFilePromptSlotSchemaLoader(promptRootDir).loadSlotSchema("incident_triage", "en"));

        assertEquals(
                "Failed to read slot schema resource: "
                        + promptRootDir.resolve("slots").resolve("incident_triage").resolve("en").resolve("slot.json"),
                exception.getMessage());
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
