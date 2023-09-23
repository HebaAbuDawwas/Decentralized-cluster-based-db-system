package com.atypon.nosqlstoragenode.storagehandler.controllers;

import com.atypon.nosqlstoragenode.storagehandler.models.DatabaseCreationRequest;
import com.atypon.nosqlstoragenode.storagehandler.models.JsonSchema;
import com.atypon.nosqlstoragenode.broadcasthandler.DatabaseBroadcastService;
import com.atypon.nosqlstoragenode.storagehandler.services.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/database")
public class DatabaseController {

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private DatabaseBroadcastService broadcastService;

    @PostMapping("/create")
    public ResponseEntity<String> createDatabase(@RequestBody DatabaseCreationRequest request, @RequestParam(value = "isBroadcast", defaultValue = "false") boolean isBroadcast) {
        String dbName = request.getDbName();
        JsonSchema schema = new JsonSchema(request.getSchema());
        try {
            databaseService.createDatabaseWithSchema(dbName, schema);
            if (!isBroadcast) broadcastService.sendBroadcastCreateDB(dbName, request.getSchema());
            return new ResponseEntity<>("Database " + dbName + " created successfully.", HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to create database " + e.getMessage() + dbName, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete/{dbName}")
    public ResponseEntity<String> deleteDatabase(@PathVariable String dbName, @RequestParam(value = "isBroadcast", defaultValue = "false") boolean isBroadcast) throws IOException {
        try {
            databaseService.deleteDatabase(dbName);
            if (!isBroadcast) broadcastService.sendBroadCastDeleteDB(dbName);
            return new ResponseEntity<>("Database deleted successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error deleting database, " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
