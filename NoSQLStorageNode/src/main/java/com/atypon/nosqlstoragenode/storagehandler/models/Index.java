package com.atypon.nosqlstoragenode.storagehandler.models;


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
    private final Map<Integer, CopyOnWriteArrayList<String>> hashTable;
    private UUID databaseId;
    private String propertyName;

    public Index() {
        this.hashTable = new ConcurrentHashMap<>();
    }

    public Index(UUID databaseId, String propertyName) {
        this.databaseId = databaseId;
        this.propertyName = propertyName;
        this.hashTable = new ConcurrentHashMap<>();
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

    private int hashFunction(String value) {
        return value.hashCode();
    }

    public void addDocumentId(String propertyValue, String documentId) {
        int hashValue = hashFunction(propertyValue);
        hashTable.putIfAbsent(hashValue, new CopyOnWriteArrayList<>());
        hashTable.get(hashValue).add(documentId);
    }

    public List<String> getDocumentIds(String propertyValue) {
        int hashValue = hashFunction(propertyValue);
        return hashTable.getOrDefault(hashValue, new CopyOnWriteArrayList<>());
    }

    public void removeDocumentId(String propertyValue, String documentId) {
        int hashValue = hashFunction(propertyValue);
        if (hashTable.containsKey(hashValue)) {
            hashTable.get(hashValue).remove(documentId);
            if (hashTable.get(hashValue).isEmpty()) {
                hashTable.remove(hashValue);
            }
        }
    }

    public void saveToDisk(String dbName) {
        Path indexPath = Paths.get("/usr/src/app/data/databases", dbName, "indexes");
        indexPath = indexPath.resolve(propertyName + "_index.json");  // Note the .json extension

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

    public UUID getDatabaseId() {
        return databaseId;
    }


    @Override
    public String toString() {
        return "Index{" + "databaseId=" + databaseId + ", propertyName='" + propertyName + '\'' + ", hashTable=" + hashTable + '}';
    }

}
