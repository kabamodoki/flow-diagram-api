package com.example.flowdiagram;

import java.util.List;

/** 入力エラー例外。 */
public class FlowDiagramException extends RuntimeException {

    private final List<String> messages;

    public FlowDiagramException(List<String> messages) {
        super(String.join(" / ", messages));
        this.messages = List.copyOf(messages);
    }

    public List<String> getMessages() {
        return messages;
    }
}
