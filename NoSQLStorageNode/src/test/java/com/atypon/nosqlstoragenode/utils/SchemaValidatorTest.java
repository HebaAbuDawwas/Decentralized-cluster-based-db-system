package com.atypon.nosqlstoragenode.utils;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaValidatorTest {

    @Test
    public void testJsonValidation() {
        // Define a JSON schema as a JSONObject using a builder pattern
        JSONObject jsonSchema = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("name", new JSONObject().put("type", "string"))
                        .put("age", new JSONObject().put("type", "integer")))
                .put("required", new JSONArray(new String[]{"name"}));  // Here's the change

        // Create a JSON object using a builder pattern that you want to validate
        JSONObject jsonData = new JSONObject()
                .put("name", "John")
                .put("age", 30);

        // Load the schema and validate
        Schema schema = SchemaLoader.load(jsonSchema);
        try {
            schema.validate(jsonData);  // throws a ValidationException if this object is invalid
            assertTrue(true, "JSON data is valid according to the schema.");
        } catch (Exception e) {
            fail("JSON data is NOT valid according to the schema.");
        }
    }
}
