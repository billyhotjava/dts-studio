package com.yuzhi.dts.copilot.ai.web.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiDataSourceUpsertRequest(
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("jdbcUrl") String jdbcUrl,
        @JsonProperty("host") String host,
        @JsonProperty("port") Integer port,
        @JsonProperty("database") String database,
        @JsonProperty("serviceName") String serviceName,
        @JsonProperty("sid") String sid,
        @JsonProperty("username") String username,
        @JsonProperty("password") String password,
        @JsonProperty("description") String description) {}
