package com.example.flowdiagram;

import java.util.List;

/** 入力 JSON の検証エラー。全違反をまとめて保持する（basic-design.md 5章）。 */
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
