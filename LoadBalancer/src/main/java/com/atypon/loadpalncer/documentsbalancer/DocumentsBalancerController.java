package com.atypon.loadpalncer.documentsbalancer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DocumentsBalancerController {

    @Autowired
    private DocumentConsistentHashing documentConsistentHashing;

    @PostMapping("/get-document-affinity")
    public ResponseEntity<String> getAffinityNodeForDocument(@RequestBody String documentId) {
        try {
            String nodeName = documentConsistentHashing.get(documentId);
            if (nodeName != null) {
                return new ResponseEntity<>(nodeName, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("No node found for document ID: " + documentId, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
