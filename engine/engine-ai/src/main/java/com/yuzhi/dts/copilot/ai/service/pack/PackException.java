package com.yuzhi.dts.copilot.ai.service.pack;

import java.util.List;

public class PackException extends RuntimeException {
    private final int status;
    private final String code;
    private final List<String> errors;

    public PackException(int status, String code, String message) {
        this(status, code, List.of(message));
    }

    public PackException(int status, String code, List<String> errors) {
        super(code);
        this.status = status;
        this.code = code;
        this.errors = List.copyOf(errors);
    }

    public int status() { return status; }
    public String code() { return code; }
    public List<String> errors() { return errors; }
}
