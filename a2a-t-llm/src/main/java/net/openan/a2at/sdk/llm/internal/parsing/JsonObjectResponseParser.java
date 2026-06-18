package net.openan.a2at.sdk.llm.internal.parsing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import net.openan.a2at.sdk.core.exception.SdkException;
import net.openan.a2at.sdk.core.json.JsonValueParser;

/**
 * Parses JSON object payloads returned by structured LLM calls.
 *
 * @since 2026-06
 */
public final class JsonObjectResponseParser implements JsonValueParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Parses one JSON object payload into a string-keyed map.
     *
     * @param payload JSON object payload
     * @return parsed values
     */
    public Map<String, Object> parse(String payload) {
        try {
            Map<String, Object> values = OBJECT_MAPPER.readValue(payload, new TypeReference<>() {
            });
            if (values == null) {
                throw new SdkException("Structured LLM payload must be a JSON object.");
            }
            return values;
        } catch (SdkException error) {
            throw error;
        } catch (Exception error) {
            throw new SdkException("Structured LLM payload must be a JSON object.", error);
        }
    }

    @Override
    public Map<String, Object> parseObject(String payload) {
        return parse(payload);
    }
}
