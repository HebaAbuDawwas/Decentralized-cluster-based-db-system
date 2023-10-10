package com.atypon.nosqlstoragenode.storagehandler.services;

import com.atypon.nosqlstoragenode.storagehandler.models.DatabaseMetadata;
import com.atypon.nosqlstoragenode.storagehandler.models.Index;
import com.atypon.nosqlstoragenode.storagehandler.models.JsonSchema;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Service
public class DatabaseService {

    private static final String BASE_DIR = "/usr/src/app/data/databases/";

    public synchronized void createDatabase(String dbName) {
        Path dbPath = Paths.get(BASE_DIR + dbName);

        if (Files.exists(dbPath)) {
            throw new RuntimeException("Database with this name already exists");
        }
        try {
            Files.createDirectories(dbPath);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void createDatabaseWithSchema(String dbName, JsonSchema schema) {
        DatabaseMetadata metadata = new DatabaseMetadata(dbName);
        createDatabase(dbName);

        File dbDirectory = new File(BASE_DIR, dbName);
        MetadataService.saveMetadataToJson(metadata, BASE_DIR + dbName + "/");
        File schemaFile = new File(dbDirectory, "schema.json");
        try (FileWriter fileWriter = new FileWriter(schemaFile)) {
            fileWriter.write(schema.getRawSchema());

            for (String property : schema.getProperties()) {
                Index index = new Index();
                index.saveToDisk(dbName,property);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save the schema to file: " + e.getMessage());
        }
    }

    public void deleteDatabase(String dbName) throws IOException {
        Path dbPath = Paths.get(BASE_DIR, dbName);
        if (Files.exists(dbPath) && Files.isDirectory(dbPath)) {
            Files.walkFileTree(dbPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            throw new IOException("Error deleting database, database not found or not a directory");
        }
    }

}
