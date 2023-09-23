package com.atypon.nosqlstoragenode.storagehandler.models;

import java.util.UUID;

public class DatabaseMetadata {
    private String dbName;
    private UUID dbUUID;

    public DatabaseMetadata(String dbName) {
        this.dbName = dbName;
        this.dbUUID = UUID.randomUUID();
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public UUID getDbUUID() {
        return dbUUID;
    }

    public void setDbUUID(UUID dbUUID) {
        this.dbUUID = dbUUID;
    }
}
