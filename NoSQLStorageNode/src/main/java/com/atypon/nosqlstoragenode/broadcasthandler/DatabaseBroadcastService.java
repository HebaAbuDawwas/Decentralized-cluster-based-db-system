package com.atypon.nosqlstoragenode.broadcasthandler;

import org.json.JSONObject;
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
public class DatabaseBroadcastService {
    private static final List<String> NODES = Arrays.asList("NoSQLStorageNode1", "NoSQLStorageNode2", "NoSQLStorageNode3", "NoSQLStorageNode4", "NoSQLStorageNode5", "NoSQLStorageNode6", "NoSQLStorageNode7", "NoSQLStorageNode8");
    private final RestTemplate restTemplate;


    @Autowired
    public DatabaseBroadcastService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public void sendBroadcastCreateDB(String dbName, String schema) {
        for (String node : NODES) {
            if (System.getenv("NODE_NAME").equals(node)) continue;

            HttpHeaders headers = createHeaders();

            JSONObject requestBodyJson = new JSONObject();
            requestBodyJson.put("dbName", dbName);
            requestBodyJson.put("schema", schema);

            HttpEntity<String> entity = new HttpEntity<>(requestBodyJson.toString(), headers);

            restTemplate.exchange("http://" + node + ":8080/database/create?isBroadcast=true", HttpMethod.POST, entity, String.class);


        }
    }

    public void sendBroadCastDeleteDB(String dbName) {
        for (String node : NODES) {
            if (System.getenv("NODE_NAME").equals(node)) continue;

            HttpHeaders headers = createHeaders();

            HttpEntity<String> entity = new HttpEntity<>(headers);

            restTemplate.exchange("http://" + node + ":8080/database/delete/" + dbName + "?isBroadcast=true", HttpMethod.DELETE, entity, String.class);


        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
