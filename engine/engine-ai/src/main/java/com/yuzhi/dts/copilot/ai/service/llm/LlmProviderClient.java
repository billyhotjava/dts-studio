package com.yuzhi.dts.copilot.ai.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface LlmProviderClient {

    JsonNode chatCompletion(String model, List<Map<String, Object>> messages,
                            Double temperature, Integer maxTokens,
                            List<Map<String, Object>> tools) throws IOException, InterruptedException;

    OpenAiCompatibleClient.StreamingChatResult chatCompletionStream(String model, List<Map<String, Object>> messages,
                                                                    Double temperature, Integer maxTokens,
                                                                    List<Map<String, Object>> tools,
                                                                    OpenAiCompatibleClient.StreamEventHandler handler)
            throws IOException, InterruptedException;

    JsonNode listModels() throws IOException, InterruptedException;

    boolean isAvailable();
}
