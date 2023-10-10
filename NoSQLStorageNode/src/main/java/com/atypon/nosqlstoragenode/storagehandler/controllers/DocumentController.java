package com.atypon.nosqlstoragenode.storagehandler.controllers;


import com.atypon.nosqlstoragenode.broadcasthandler.DocumentBroadcastService;
import com.atypon.nosqlstoragenode.storagehandler.models.UpdatePropertiesRequest;
import com.atypon.nosqlstoragenode.storagehandler.services.DocumentService;
import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/database/{db_name}")
public class DocumentController {

    @Autowired
    private DocumentService documentService;
    @Autowired
    private DocumentBroadcastService broadcastService;


    @PostMapping("/document/create")
    public ResponseEntity<String> createDocument(@PathVariable("db_name") String dbName, @RequestBody String jsonDocument, @RequestParam(value = "isBroadcast", defaultValue = "false") boolean isBroadcast, @RequestParam(value = "documentId", required = false, defaultValue = "") String documentId) {
        try {
            if (!isBroadcast) {
                String docId = documentService.createDocument(dbName, jsonDocument);
                broadcastService.sendBroadCastCreateDocument(dbName, docId, jsonDocument);

            } else {
                documentService.createDocumentWithProvidedId(dbName, jsonDocument, documentId);
            }
            return ResponseEntity.ok("Document created successfully");
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Failed to create document, " + e.getMessage());
        }
    }

    @RequestMapping(value = "/document/{documentId}/delete", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteDocument(@PathVariable("db_name") String dbName, @PathVariable String documentId, @RequestParam(value = "isBroadcast", defaultValue = "false") boolean isBroadcast) {
        try {
            documentService.deleteDocument(dbName, documentId);
            if (!isBroadcast) broadcastService.sendBroadCastDeleteDocument(dbName, documentId);
            return ResponseEntity.ok("document deleted");
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Failed to delete document");
        }
    }

    @GetMapping("/document/get-all")
    public Map<String, JsonNode> getAllDocuments(@PathVariable("db_name") String dbName) throws IOException {
        Map<String, JsonNode> documents = documentService.getAllDocuments(dbName);

        for (Map.Entry<String, JsonNode> entry : documents.entrySet()) {
            System.out.println("Document ID: " + entry.getKey());
            System.out.println("Document Data: " + entry.getValue().toPrettyString());
        }

        return documents;
    }

    @GetMapping("/document/{documentId}/read-properties")
    public ResponseEntity<Map<String, Object>> readDocumentProperties(@PathVariable("db_name") String dbName, @PathVariable String documentId, @RequestParam List<String> propertiesToRead) {
        try {
            Map<String, Object> documentProperties = documentService.readDocumentProperties(dbName, documentId, propertiesToRead);
            return ResponseEntity.ok(documentProperties);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @RequestMapping(value = "/document/{documentId}/write-properties", method = RequestMethod.PUT)
    public ResponseEntity<?> updateDocumentProperties(@PathVariable("db_name") String dbName, @PathVariable String documentId, @RequestBody UpdatePropertiesRequest propertiesToUpdate, @RequestParam(value = "isBroadcast", defaultValue = "false") boolean isBroadcast, @RequestParam(value = "updatedDocumentId", required = false, defaultValue = "") String updatedDocumentId) {
        try {
            if (isBroadcast) {
                documentService.updateDocumentPropertiesWithNewId(dbName, documentId, new JSONObject(propertiesToUpdate.getProperties()), updatedDocumentId);

            } else {
                String documentAffinity = documentService.getDocumentAffinity(documentId);
                System.out.println("document affinity : " + documentAffinity);
                if(System.getenv("NODE_NAME").equals(documentAffinity)){
                    System.out.println("yes this is affinity");
                String updatedDocId = documentService.updateDocumentProperties(dbName, documentId, new JSONObject(propertiesToUpdate.getProperties()));
                broadcastService.sendBroadcastUpdateDocumentProperties(dbName, documentId, propertiesToUpdate, updatedDocId);}
                else{
                    documentService.forwardRequestToAffinity(documentId, propertiesToUpdate, documentAffinity,dbName);
                }
            }
            return new ResponseEntity<>(HttpStatus.OK);


        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
