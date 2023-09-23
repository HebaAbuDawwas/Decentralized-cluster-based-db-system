package com.atypon.nosqlstoragenode.storagehandler.services;

import com.atypon.nosqlstoragenode.storagehandler.models.DatabaseMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class MetadataService {

    public static void saveMetadataToJson(DatabaseMetadata metadata, String path) {
        File metadataFile = new File(path, "metadata.json");
        try (FileWriter writer = new FileWriter(metadataFile)) {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(metadata);
            writer.write(jsonString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
