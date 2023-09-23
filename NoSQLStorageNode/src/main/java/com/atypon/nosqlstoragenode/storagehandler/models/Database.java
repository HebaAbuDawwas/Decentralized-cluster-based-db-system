package com.atypon.nosqlstoragenode.storagehandler.models;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class Database {

    private final UUID id;
    private final String name;
    private final JsonSchema schema;
    private final List<String> documentsIds;

    public Database(String name, JsonSchema schema) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.schema = schema;
        this.documentsIds = new CopyOnWriteArrayList<>();
    }


    @Override
    public String toString() {
        return "Database{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", schema=" + schema +
                ", documentsIds=" + documentsIds +
                '}';
    }
}
