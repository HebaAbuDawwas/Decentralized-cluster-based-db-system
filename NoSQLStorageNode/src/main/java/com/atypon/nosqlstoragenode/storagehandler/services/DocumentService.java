package com.atypon.nosqlstoragenode.storagehandler.services;


import com.atypon.nosqlstoragenode.storagehandler.models.Index;
import com.atypon.nosqlstoragenode.storagehandler.models.JsonSchema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentService {

    private static final String BASE_DIR = "/usr/src/app/data/databases/";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public synchronized String createDocument(String dbName, String jsonDocument) throws IOException {
        JsonSchema schema = loadSchemaFromFile(dbName);
        if (schema == null) {
            throw new RuntimeException("Failed to load schema for database: " + dbName);
        }

        if (!schema.validateDocument(jsonDocument)) {
            throw new RuntimeException("Document validation failed");
        }

        JSONObject document = new JSONObject(jsonDocument);
        String docId = UUID.randomUUID().toString();

        for (String property : schema.getProperties()) {
            String propertyValue = document.optString(property);
            if (propertyValue != null) {
                Index index = Index.loadFromDisk(dbName, property);
                if (index != null) {
                    index.addDocumentId(propertyValue, docId);
                    index.saveToDisk(dbName);
                }
            }
        }
        saveDocumentToDatabase(dbName, jsonDocument, docId);
        return docId;
    }


    public Map<String, Object> readDocumentProperties(String dbName, String documentId, List<String> propertiesToRead) throws IOException {
        Path documentPath = Paths.get(BASE_DIR + dbName + "/documents/" + documentId + ".json");

        if (!Files.exists(documentPath)) {
            throw new IOException("Document not found");
        }

        byte[] documentBytes = Files.readAllBytes(documentPath);
        JSONObject document = new JSONObject(new String(documentBytes));

        Map<String, Object> selectedProperties = new HashMap<>();
        for (String property : propertiesToRead) {
            Object propertyValue = document.opt(property);
            if (propertyValue != null) {
                selectedProperties.put(property, propertyValue);
            }
        }

        return selectedProperties;
    }

    private JsonSchema loadSchemaFromFile(String dbName) {
        Path schemaPath = Paths.get(BASE_DIR, dbName, "schema.json");
        try {
            String rawSchema = new String(Files.readAllBytes(schemaPath));
            return new JsonSchema(rawSchema);
        } catch (IOException e) {
            System.err.println("Error reading schema file: " + e.getMessage());
            return null;
        }
    }

    private synchronized void saveDocumentToDatabase(String dbName, String jsonDocument, String docId) throws IOException {
        String documentsDir = BASE_DIR + dbName + "/documents/";
        File documentsDirectory = new File(documentsDir);
        if (!documentsDirectory.exists()) {
            documentsDirectory.mkdirs();
        }
        File documentFile = new File(documentsDir, docId + ".json");

        try (FileWriter fileWriter = new FileWriter(documentFile)) {
            fileWriter.write(jsonDocument);
        } catch (IOException e) {
            throw new IOException("Error saving document to disk: " + e.getMessage());
        }
    }

    public void deleteDocument(String dbName, String documentId) throws IOException {
        Path dbPath = Paths.get(BASE_DIR + dbName);
        if (!Files.exists(dbPath)) {
            throw new IllegalArgumentException("Database not found");
        }

        Map<String, Object> documentProperties = getDocumentProperties(dbName, documentId);
        if (documentProperties == null) {
            throw new IllegalArgumentException("Document not found");

        }

        for (Map.Entry<String, Object> entry : documentProperties.entrySet()) {
            String propertyName = entry.getKey();
            String propertyValue = String.valueOf(entry.getValue());
            Index index = Index.loadFromDisk(dbName, propertyName);
            if (index != null) {
                index.removeDocumentId(propertyValue, documentId);
                index.saveToDisk(dbName);
            } else {
                System.err.println("Index not found for property: " + propertyName);
            }
        }


        Path documentPath = Paths.get(BASE_DIR + dbName + "/documents/" + documentId + ".json");
        try {
            Files.delete(documentPath);
        } catch (IOException e) {
            throw new IOException("Error deleting document: " + e.getMessage());
        }
    }

    public Map<String, Object> getDocumentProperties(String dbName, String documentId) {
        Path documentPath = Paths.get(BASE_DIR + dbName + "/documents/" + documentId + ".json");
        if (!Files.exists(documentPath)) {
            System.err.println("Document not found");
            return null;
        }

        try {
            byte[] documentBytes = Files.readAllBytes(documentPath);
            return objectMapper.readValue(documentBytes, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            System.err.println("Error reading document: " + e.getMessage());
            return null;
        }
    }

    public Map<String, JsonNode> getAllDocuments(String dbName) throws IOException {
        Map<String, JsonNode> documents = new HashMap<>();
        Path documentsDir = Paths.get(BASE_DIR + dbName + "/documents");

        if (Files.exists(documentsDir) && Files.isDirectory(documentsDir)) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(documentsDir, "*.json")) {
                for (Path documentPath : directoryStream) {
                    String documentId = documentPath.getFileName().toString().replace(".json", "");
                    byte[] documentBytes = Files.readAllBytes(documentPath);
                    JsonNode documentData = objectMapper.readTree(documentBytes);
                    documents.put(documentId, documentData);
                }
            }
        } else {
            System.err.println("Documents directory not found or not a directory");
        }

        return documents;
    }


    public synchronized String updateDocumentProperties(String dbName, String documentId, JSONObject propertiesToUpdate) throws IOException {
        Path documentPath = Paths.get(BASE_DIR + dbName + "/documents/" + documentId + ".json");
        if (!Files.exists(documentPath)) {
            throw new IllegalArgumentException("Document not found");
        }
        try {
            byte[] documentBytes = Files.readAllBytes(documentPath);
            JSONObject existingDocument = new JSONObject(new String(documentBytes, StandardCharsets.UTF_8));

            for (String key : propertiesToUpdate.keySet()) {
                if (propertiesToUpdate.isNull(key)) {
                    existingDocument.remove(key);
                } else {
                    existingDocument.put(key, propertiesToUpdate.get(key));
                }
            }
            deleteDocument(dbName, documentId);
            return createDocument(dbName, existingDocument.toString());
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("Error updating document: " + e.getMessage());
        }
    }

    public synchronized void updateDocumentPropertiesWithNewId(String dbName, String documentId, JSONObject propertiesToUpdate, String updatedDocumentId) throws IOException {
        Path documentPath = Paths.get(BASE_DIR + dbName + "/documents/" + documentId + ".json");
        if (!Files.exists(documentPath)) {
            throw new IllegalArgumentException("Document not found");
        }
        try {
            byte[] documentBytes = Files.readAllBytes(documentPath);
            JSONObject existingDocument = new JSONObject(new String(documentBytes, StandardCharsets.UTF_8));

            for (String key : propertiesToUpdate.keySet()) {
                if (propertiesToUpdate.isNull(key)) {
                    existingDocument.remove(key);
                } else {
                    existingDocument.put(key, propertiesToUpdate.get(key));
                }
            }
            deleteDocument(dbName, documentId);
            createDocumentWithProvidedId(dbName, existingDocument.toString(), updatedDocumentId);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("Error updating document: " + e.getMessage());
        }
    }


    public void createDocumentWithProvidedId(String dbName, String jsonDocument, String documentId) throws IOException {
        JsonSchema schema = loadSchemaFromFile(dbName);
        if (schema == null) {
            throw new RuntimeException("Failed to load schema for database: " + dbName);
        }

        if (!schema.validateDocument(jsonDocument)) {
            throw new RuntimeException("Document validation failed");
        }

        JSONObject document = new JSONObject(jsonDocument);

        for (String property : schema.getProperties()) {
            String propertyValue = document.optString(property);
            if (propertyValue != null) {
                Index index = Index.loadFromDisk(dbName, property);
                if (index != null) {
                    index.addDocumentId(propertyValue, documentId);
                    index.saveToDisk(dbName);
                }
            }
        }
        saveDocumentToDatabase(dbName, jsonDocument, documentId);
    }
}
