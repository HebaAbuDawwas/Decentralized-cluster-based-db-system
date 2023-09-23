package com.atypon.nosqlstoragenode.storagehandler.models;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JsonSchema {

    private String rawSchema;
    private Schema everitSchema;

    public JsonSchema(String rawSchema) {
        this.rawSchema = rawSchema;
        this.everitSchema = loadEveritSchema(rawSchema);
    }

    private Schema loadEveritSchema(String rawSchema) {
        JSONObject jsonSchema = new JSONObject(rawSchema);
        return SchemaLoader.load(jsonSchema);
    }

    public String getRawSchema() {
        return rawSchema;
    }

    public void setRawSchema(String rawSchema) {
        this.rawSchema = rawSchema;
        this.everitSchema = loadEveritSchema(rawSchema);
    }


    public boolean validateDocument(String jsonDocument) {
        JSONObject jsonObject = new JSONObject(jsonDocument);
        try {
            everitSchema.validate(jsonObject);
            return true;
        } catch (ValidationException e) {
            System.out.println("JSON validation error: " + e.getMessage());
            return false;
        }
    }

    public List<String> getProperties() {
        JSONObject schemaObject = new JSONObject(rawSchema);
        if (!schemaObject.has("properties")) {
            return Collections.emptyList();
        }

        JSONObject propertiesObject = schemaObject.getJSONObject("properties");
        return new ArrayList<>(propertiesObject.keySet());
    }


    @Override
    public String toString() {
        return "JsonSchema{" + "rawSchema='" + rawSchema + '\'' + ", everitSchema=" + everitSchema + '}';
    }

}
