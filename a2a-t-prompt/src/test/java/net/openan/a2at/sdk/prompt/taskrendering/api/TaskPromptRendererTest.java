package net.openan.a2at.sdk.prompt.taskrendering.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.openan.a2at.sdk.prompt.taskrendering.exception.TaskPromptRenderException;
import org.junit.jupiter.api.Test;

class TaskPromptRendererTest {

    private final TaskPromptRenderer renderer = new TaskPromptRenderer();

    @Test
    void renderBuildsPlainPromptBody() {
        String prompt = renderer.render(
                "Site: {site}\nNotes: {additional_notes}", Map.of("site", "Site A", "additional_notes", ""));

        assertEquals("Site: Site A\nNotes: ", prompt);
    }

    @Test
    void renderSupportsDoubleBracedPlaceholders() {
        String prompt = renderer.render(
                "Topic: {{topic}}\nCondition: {{condition}}",
                Map.of("topic", "Incident", "condition", "critical alert"));

        assertEquals("Topic: Incident\nCondition: critical alert", prompt);
    }

    @Test
    void renderSupportsNonAsciiPlaceholders() {
        String prompt = renderer.render(
                "通知主题: {{通知主题}}\n订阅条件: {{订阅条件}}",
                Map.of("通知主题", "Incident", "订阅条件", "故障优先级为：严重"));

        assertEquals("通知主题: Incident\n订阅条件: 故障优先级为：严重", prompt);
    }

    @Test
    void renderCollapsesSectionBodyWhenFirstEffectiveLineIsStandaloneSlotWithEnglishSuffix() {
        String prompt = renderer.render(
                "## Task Type\n"
                        + "Diagnosis\n\n"
                        + "## Task Target\n"
                        + "{{task_target}}(Required)\n\n"
                        + "Requirement: explain the target.\n"
                        + "Example: complete the diagnosis.\n\n"
                        + "## Expected Output\n"
                        + "{{expected_output}}(Optional)\n",
                Map.of(
                        "task_target", "Complete the diagnosis and provide remediation advice.",
                        "expected_output", "Return a structured diagnosis result."));

        assertEquals(
                "## Task Type\n"
                        + "Diagnosis\n\n"
                        + "## Task Target\n"
                        + "Complete the diagnosis and provide remediation advice.\n\n"
                        + "## Expected Output\n"
                        + "Return a structured diagnosis result.\n",
                prompt);
    }

    @Test
    void renderCollapsesSectionBodyWhenFirstEffectiveLineIsStandaloneSlotWithChineseSuffix() {
        String prompt = renderer.render(
                "## 任务目标\n"
                        + "{{task_target}}（必选）\n\n"
                        + "要求：说明目标。\n"
                        + "示例：完成诊断。\n\n"
                        + "## 期望输出\n"
                        + "{{expected_output}}（可选）\n",
                Map.of(
                        "task_target", "完成故障诊断并提供处置建议。",
                        "expected_output", "返回结构化诊断结果。"));

        assertEquals(
                "## 任务目标\n"
                        + "完成故障诊断并提供处置建议。\n\n"
                        + "## 期望输出\n"
                        + "返回结构化诊断结果。\n",
                prompt);
    }

    @Test
    void renderPreservesRegularInlinePlaceholderContent() {
        String prompt = renderer.render(
                "## Subscription\n"
                        + "Please subscribe to {{topic}} incidents.\n\n"
                        + "## Condition\n"
                        + "{{condition}}（可选）\n"
                        + "Requirement: describe the filter.\n",
                Map.of("topic", "network", "condition", "critical only"));

        assertEquals(
                "## Subscription\n"
                        + "Please subscribe to network incidents.\n\n"
                        + "## Condition\n"
                        + "critical only\n",
                prompt);
    }

    @Test
    void renderRaisesWhenTemplateReferencesUnknownSlot() {
        assertThrows(
                TaskPromptRenderException.class,
                () -> renderer.render("Site: {site}\nTime Range: {time_range}", Map.of("site", "Site A")));
    }
}
