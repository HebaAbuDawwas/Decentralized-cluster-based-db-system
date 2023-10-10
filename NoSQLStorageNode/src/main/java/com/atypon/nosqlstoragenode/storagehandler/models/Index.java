package com.atypon.nosqlstoragenode.storagehandler.models;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Index {
    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Map<String, CopyOnWriteArrayList<String>> hashTable;

    public Index() {
        this.hashTable = new ConcurrentHashMap<>();
    }

    @JsonCreator
    public Index(@JsonProperty("hashTable") Map<String, CopyOnWriteArrayList<String>> hashTable) {
        this.hashTable = new ConcurrentHashMap<>(hashTable);
    }

    public static Index loadFromDisk(String dbName, String propertyName) {
        Path indexPath = Paths.get("/usr/src/app/data/databases", dbName, "indexes", propertyName + "_index.json");
        try {
            String json = new String(Files.readAllBytes(indexPath));
            return objectMapper.readValue(json, Index.class);
        } catch (IOException e) {
            System.err.println("Error reading index from disk: " + e.getMessage());
            return null;
        }
    }


    public void addDocumentId(String propertyValue, String documentId) {
        hashTable.putIfAbsent(propertyValue, new CopyOnWriteArrayList<>());
        hashTable.get(propertyValue).add(documentId);
    }

    public void removeDocumentId(String propertyValue, String documentId) {
        if (hashTable.containsKey(propertyValue)) {
            hashTable.get(propertyValue).remove(documentId);
            if (hashTable.get(propertyValue).isEmpty()) {
                hashTable.remove(propertyValue);
            }
        }
    }

    public void saveToDisk(String dbName, String propertyName) {
        Path indexPath = Paths.get("/usr/src/app/data/databases", dbName, "indexes");
        indexPath = indexPath.resolve(propertyName + "_index.json");

        File indexFile = indexPath.toFile();
        if (!indexFile.getParentFile().exists() && !indexFile.getParentFile().mkdirs()) {
            System.err.println("Failed to create necessary directories for: " + indexFile.getPath());
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(this);
            Files.write(indexPath, json.getBytes());
        } catch (IOException e) {
            System.err.println("Error saving index to disk: " + e.getMessage());
        }
    }

    public Map<String, CopyOnWriteArrayList<String>> getHashTable() {
        return this.hashTable;
    }
}