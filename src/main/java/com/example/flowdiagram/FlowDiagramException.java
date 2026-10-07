package com.example.flowdiagram;

import java.util.List;

/** 入力データが不正なときに投げる例外。違反内容を複数まとめて持てる。 */
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
