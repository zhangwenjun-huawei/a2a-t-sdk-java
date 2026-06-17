package net.openan.a2at.sdk.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class LlmPackageStructureTest {

    @Test
    void rootLlmPackageOnlyKeepsDocumentationSources() throws IOException {
        Path root = Path.of("src", "main", "java", "net", "openan", "a2at", "sdk", "llm");
        assertEquals(List.of("LLMClient.java", "package-info.java"), topLevelJavaFiles(root));
    }

    @Test
    void llmSubpackagesFollowCapabilityAndInternalSplit() throws IOException {
        Path root = Path.of("src", "main", "java", "net", "openan", "a2at", "sdk", "llm");

        assertEquals(List.of("LLMAdapter.java", "OpenAICompatibleAdapter.java"), topLevelJavaFiles(root.resolve("adapter")));
        assertEquals(
                List.of("LLMResponse.java", "LlmUsage.java", "StructuredGenerationRequest.java"),
                topLevelJavaFiles(root.resolve("model")));
        assertEquals(List.of("LlmConfigException.java"), topLevelJavaFiles(root.resolve("exception")));
        assertEquals(List.of("LlmClientConfig.java", "StructuredLlmRuntimeConfig.java"), topLevelJavaFiles(root.resolve("config")));
        assertEquals(
                List.of("JsonObjectResponseParser.java"),
                topLevelJavaFiles(root.resolve("internal").resolve("parsing")));
        assertEquals(
                List.of("InMemoryLlmSessionStore.java", "LlmSessionStore.java"),
                topLevelJavaFiles(root.resolve("internal").resolve("session")));
        assertEquals(List.of("DefaultLlmClientBuilder.java"), topLevelJavaFiles(root.resolve("internal").resolve("assembly")));
        assertEquals(
                List.of(
                        "OpenAiSdkResponseExecutor.java",
                        "OpenAiSdkStructuredRequestMapper.java",
                        "OpenAiSdkStructuredResponseMapper.java"),
                topLevelJavaFiles(root.resolve("internal").resolve("openai")));

        assertFalse(Files.exists(root.resolve("StructuredLlmClient.java")));
        assertFalse(Files.exists(root.resolve("LLMAdapter.java")));
        assertFalse(Files.exists(root.resolve("OpenAICompatibleAdapter.java")));
        assertFalse(Files.exists(root.resolve("LLMResponse.java")));
        assertFalse(Files.exists(root.resolve("LlmUsage.java")));
        assertFalse(Files.exists(root.resolve("StructuredGenerationRequest.java")));
        assertFalse(Files.exists(root.resolve("internal").resolve("legacy")));
        assertFalse(Files.exists(root.resolve("internal").resolve("legacy").resolve("StructuredLlmRequest.java")));
        assertFalse(Files.exists(root.resolve("internal").resolve("legacy").resolve("OpenAiCompatibleClientConfig.java")));
        assertFalse(Files.exists(root.resolve("internal").resolve("legacy").resolve("LegacyStructuredLlmClientAdapter.java")));
        assertFalse(Files.exists(root.resolve("internal").resolve("openai").resolve("OpenAiCompatibleTransport.java")));
        assertFalse(Files.exists(root.resolve("internal").resolve("openai").resolve("OpenAiCompatiblePayloadBuilder.java")));
        assertFalse(Files.exists(root.resolve("internal").resolve("openai").resolve("OpenAiCompatibleResponseParser.java")));
        assertFalse(Files.exists(root.resolve("internal").resolve("openai").resolve("OpenAiCompatibleStructuredLlmClient.java")));
        assertFalse(Files.exists(root.resolve("spi")));
        assertFalse(Files.exists(root.resolve("api")));
        assertTrue(Files.exists(root.resolve("adapter")));
        assertTrue(Files.exists(root.resolve("model")));
        assertTrue(Files.exists(root.resolve("exception")));
        assertTrue(Files.exists(root.resolve("config")));
        assertTrue(Files.exists(root.resolve("internal").resolve("assembly")));
        assertTrue(Files.exists(root.resolve("internal").resolve("openai")));
        assertTrue(Files.exists(root.resolve("internal").resolve("parsing")));
        assertTrue(Files.exists(root.resolve("internal").resolve("session")));
        assertTrue(Files.exists(root.resolve("LLMClient.java")));
        assertTrue(Files.exists(root.resolve("adapter").resolve("LLMAdapter.java")));
        assertTrue(Files.exists(root.resolve("adapter").resolve("OpenAICompatibleAdapter.java")));
        assertTrue(Files.exists(root.resolve("model").resolve("LLMResponse.java")));
        assertTrue(Files.exists(root.resolve("model").resolve("LlmUsage.java")));
        assertTrue(Files.exists(root.resolve("model").resolve("StructuredGenerationRequest.java")));
        assertTrue(Files.exists(root.resolve("exception").resolve("LlmConfigException.java")));
        assertTrue(Files.exists(root.resolve("internal").resolve("session").resolve("LlmSessionStore.java")));
        assertTrue(Files.exists(root.resolve("internal").resolve("parsing").resolve("JsonObjectResponseParser.java")));
        assertTrue(Files.exists(root.resolve("internal").resolve("session").resolve("InMemoryLlmSessionStore.java")));
        assertTrue(Files.exists(root.resolve("internal").resolve("assembly").resolve("DefaultLlmClientBuilder.java")));
        assertTrue(Files.exists(root.resolve("internal").resolve("openai").resolve("OpenAiSdkResponseExecutor.java")));
        assertTrue(Files.exists(root.resolve("internal").resolve("openai").resolve("OpenAiSdkStructuredRequestMapper.java")));
        assertTrue(Files.exists(root.resolve("internal").resolve("openai").resolve("OpenAiSdkStructuredResponseMapper.java")));
    }

    private static List<String> topLevelJavaFiles(Path path) throws IOException {
        return Files.list(path)
                .filter(file -> file.getFileName().toString().endsWith(".java"))
                .map(file -> file.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());
    }
}
