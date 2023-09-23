package com.atypon.nosqlstoragenode.broadcasthandler;

import com.atypon.nosqlstoragenode.storagehandler.models.UpdatePropertiesRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class DocumentBroadcastService {
    private static final List<String> NODES = Arrays.asList("NoSQLStorageNode1", "NoSQLStorageNode2", "NoSQLStorageNode3", "NoSQLStorageNode4", "NoSQLStorageNode5", "NoSQLStorageNode6", "NoSQLStorageNode7", "NoSQLStorageNode8");
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;


    @Autowired
    public DocumentBroadcastService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;

    }


    public void sendBroadCastDeleteDocument(String dbName, String documentId) {
        for (String node : NODES) {
            if (System.getenv("NODE_NAME").equals(node)) continue;

            HttpHeaders headers = createHeaders();

            HttpEntity<String> entity = new HttpEntity<>(headers);

            restTemplate.exchange("http://" + node + ":8080/database/" + dbName + "/document/" + documentId + "/delete?isBroadcast=true", HttpMethod.DELETE, entity, String.class);

        }
    }

    public void sendBroadCastCreateDocument(String dbName, String documentId, String jsonDocument) {
        for (String node : NODES) {
            if (System.getenv("NODE_NAME").equals(node)) continue;

            HttpHeaders headers = createHeaders();

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonDocument, headers);

            restTemplate.exchange("http://" + node + ":8080/database/" + dbName + "/document/create?isBroadcast=true&documentId=" + documentId, HttpMethod.POST, requestEntity, String.class);

        }
    }


    public void sendBroadcastUpdateDocumentProperties(String dbName, String documentId, UpdatePropertiesRequest propertiesToUpdate, String updatedDocumentId) throws JsonProcessingException {
        for (String node : NODES) {
            if (System.getenv("NODE_NAME").equals(node)) continue;

            HttpHeaders headers = createHeaders();
            String requestBody = objectMapper.writeValueAsString(propertiesToUpdate);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            String url = "http://" + node + ":8080/database/" + dbName + "/document/" + documentId + "/write-properties?isBroadcast=true&updatedDocumentId=" + updatedDocumentId;

            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
