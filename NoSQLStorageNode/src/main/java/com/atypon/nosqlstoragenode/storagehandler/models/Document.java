package com.atypon.nosqlstoragenode.storagehandler.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class Document {

    @JsonProperty("id")
    private UUID id;
    @JsonProperty("content")
    private String content;


    public Document() {
        this.id = UUID.randomUUID();
    }

    public Document(String content) {
        this.id = UUID.randomUUID();
        this.content = content;
    }

    public Document(UUID id, String content) {
        this.id = id;
        this.content = content;
    }

    @Override
    public String toString() {
        return "Document{" + "id=" + id + ", content='" + content + '\'' + '}';
    }
}
