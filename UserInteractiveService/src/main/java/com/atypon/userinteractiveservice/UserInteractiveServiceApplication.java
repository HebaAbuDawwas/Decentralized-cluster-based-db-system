package com.atypon.userinteractiveservice;

import com.atypon.userinteractiveservice.utils.usersmysql.UsersMySQLUtils;
import com.atypon.userinteractiveservice.utils.usersmysql.models.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import redis.clients.jedis.Jedis;

import java.io.Console;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@SpringBootApplication
public class UserInteractiveServiceApplication {
    static Jedis jedisUserNode = new Jedis("redis://redis:6379");


    public static void main(String[] args) throws IOException {

        SpringApplication.run(UserInteractiveServiceApplication.class, args);
        //run docker attach UserInteractionService in cmd to start this:
        Console console = System.console();
        if (console == null) {
            System.err.println("Couldn't get Console instance. Running in an environment where console is not supported.");
            return;
        }


        boolean loggedIn = false;

        System.out.println("Welcome to UserInteractiveService.");
        System.out.println("Type 'exit' to stop the service.");
        System.out.println("If you don't have an account, type 'register' to create one.\n");
        User user = new User();
        user.setPassword("pass");
        user.setUsername("user");
        registerUser(user);
        user= new User();

        String assignedNode = null;
        while (true) {
            if (!loggedIn) {
                System.out.println("Please log in or register.");
                String username = console.readLine("Username: ");
                user.setUsername(username);

                if ("exit".equalsIgnoreCase(username)) {
                    break;
                }

                if ("register".equalsIgnoreCase(username)) {
                    String newUsername = console.readLine("Choose your username: ");
                    char[] newPasswordArray = console.readPassword("Choose your password: ");
                    String newPassword = new String(newPasswordArray);
                    user.setPassword(newPassword);
                    user.setUsername(newUsername);
                    registerUser(user);
                    continue;
                }
                char[] passwordArray = console.readPassword("Password: ");
                String password = new String(passwordArray);
                user.setPassword(password);
                if(checkUserExistence(username)){
                    assignedNode = getAssignedNode(username);
                    System.out.println(user.getUsername() + " is assigned to: " + assignedNode);
                    if(checkUserPassword(user,assignedNode)){
                         loggedIn = true;
                    } else {
                        continue;
                    }
                } else {
                    System.out.println("Error: Username does not exists.");
                    continue;
                }
            }
            System.out.println("Choose an option to do: ");
            System.out.println("1. Create database");
            System.out.println("2. Delete database");
            System.out.println("3. Use database");
            String choice = console.readLine("> ");
            String database;
            if ("exit".equalsIgnoreCase(choice)) {
                break;
            }
            switch (choice) {
                case "1":
                    System.out.print("Enter the name of the new database: ");
                    String dbName = console.readLine();
                    System.out.print("Enter the schema for the new database: ");
                    String schema = readSchema(console);
                    createDatabase(dbName, schema,assignedNode);
                    continue;
                case "2":
                    System.out.print("Enter the name of the database to delete: ");
                    dbName = console.readLine();
                    deleteDatabase(dbName, assignedNode);
                    continue;
                case "3":
                    System.out.println("db name:");
                    dbName = console.readLine();
                    database = dbName;
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
                    continue;
            }
            while (true) {
                getAllDocuments(database,assignedNode);
                System.out.println("Choose an option to do: ");
                System.out.println("1. create document");
                System.out.println("2. read document properties");
                System.out.println("3. update document properties");
                System.out.println("4. delete document");
                 choice = console.readLine();

                switch (choice) {
                    case "1":
                        System.out.print("Enter JSON document: ");
                        String jsonDocument = console.readLine();
                        createDocument(jsonDocument, assignedNode, database);
                        break;

                    case "2":
                        System.out.print("Enter document ID: ");
                        String docIdToRead = console.readLine();
                        List<String> propertiesToRead = new ArrayList<>();
                        while (true) {
                            System.out.print("Enter property to read (or 'done' to finish): ");
                            String property = console.readLine();
                            if (property.equalsIgnoreCase("done")) break;

                            propertiesToRead.add(property);
                        }
                        readDocumentProperties(docIdToRead, assignedNode, database, propertiesToRead);
                        break;

                    case "3":
                        System.out.print("Enter document ID: ");
                        String docIdToUpdate = console.readLine();
                        System.out.println("updated props:");
                        String updatedProps = console.readLine();
                        updateDocumentProperties(docIdToUpdate, convertJsonToUpdatePropertiesRequest(updatedProps), assignedNode, database);
                        break;

                    case "4":
                        System.out.print("Enter document ID: ");
                        String docIdToDelete = console.readLine();
                        deleteDocument(docIdToDelete, assignedNode, database);
                        break;

                    default:
                        System.out.println("Invalid choice. Try again.");
                        break;
                }
            }
        }

        }
    public static UpdatePropertiesRequest convertJsonToUpdatePropertiesRequest(String jsonString) throws IOException {
        UpdatePropertiesRequest updatePropertiesRequest = new UpdatePropertiesRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonString);

        if (jsonNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fieldsIterator = jsonNode.fields();
            while (fieldsIterator.hasNext()) {
                Map.Entry<String, JsonNode> field = fieldsIterator.next();
                String key = field.getKey();
                Object value;
                if (field.getValue().isTextual()) {
                    value = field.getValue().asText();
                } else if (field.getValue().isNumber()) {
                    value = field.getValue().numberValue();
                } else {
                    value = field.getValue().toString();
                }
                updatePropertiesRequest.setProperty(key, value);
            }
        }

        return updatePropertiesRequest;
    }


    private static void createDocument(String jsonDocument, String node, String dbName) {
        String url = "http://" + node + ":8080/database/" + dbName + "/document/create";
        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonDocument, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println(response.getBody());
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private static void readDocumentProperties(String docId, String node, String dbName, List<String> propertiesToRead) {
        RestTemplate restTemplate = new RestTemplate();

        String url = "http://" + node + ":8080/database/" + dbName + "/document/" + docId + "/read-properties";

        String propertiesString = String.join(",", propertiesToRead);

        UriComponents uriComponents;
        uriComponents = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("propertiesToRead", propertiesString)
                .build();

        ResponseEntity<Map> response = restTemplate.getForEntity(uriComponents.toUri(), Map.class);

        System.out.println(response.getBody());
    }



    private static void updateDocumentProperties(String docId, UpdatePropertiesRequest propertiesToUpdate, String node, String dbName) {
        try {
            String url = "http://" + node + ":8080/database/" + dbName + "/document/" + docId + "/write-properties";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonPropertiesToUpdate = objectMapper.writeValueAsString(propertiesToUpdate.getProperties());

            HttpEntity<String> entity = new HttpEntity<>(jsonPropertiesToUpdate, headers);
            restTemplate.put(url, entity);
            System.out.println("Properties updated.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to update properties: " + e.getMessage());
        }
    }
    private static void deleteDocument(String docId, String node, String dbName) {
        HttpHeaders headers = createHeaders();
        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<String> entity = new HttpEntity<>(headers);

        restTemplate.exchange("http://" + node + ":8080/database/" + dbName + "/document/" + docId + "/delete", HttpMethod.DELETE, entity, String.class);

    }

    private static void getAllDocuments(String dbName, String node) {
        RestTemplate restTemplate = new RestTemplate();

        String url = "http://" + node + ":8080/database/" + dbName + "/document/get-all";

        ResponseEntity<Map<String, JsonNode>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, JsonNode>>() {}
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, JsonNode> documents = response.getBody();
            if (documents != null) {
                for (Map.Entry<String, JsonNode> entry : documents.entrySet()) {
                    System.out.println("Document ID: " + entry.getKey());
                    System.out.println("Document Data: " + entry.getValue().toPrettyString());
                }
            } else {
                System.err.println("No documents found.");
            }
        } else {
            System.err.println("Failed to fetch documents: " + response.getStatusCodeValue());
        }
    }
    private static void deleteDatabase(String dbName, String node) {
        HttpHeaders headers = createHeaders();
        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "http://" + node + ":8080/database/delete/" + dbName,
                HttpMethod.DELETE, entity, String.class);

        System.out.println(response.getBody());
    }

    private static String readSchema(Console console) {
        JSONObject schemaJson = new JSONObject();
        schemaJson.put("type", "object");
        JSONObject propertiesJson = new JSONObject();
        JSONArray requiredArray = new JSONArray();

        while (true) {
            System.out.print("Enter a schema attribute name (or 'done' to finish): ");
            String attributeName = console.readLine();
            if (attributeName.equalsIgnoreCase("done")) {
                break;
            }
            System.out.print("Enter the type for attribute '" + attributeName + "' (e.g., string, number): ");
            String attributeType = console.readLine();
            JSONObject attributeJson = new JSONObject();
            attributeJson.put("type", attributeType);
            propertiesJson.put(attributeName, attributeJson);

            System.out.print("Is the attribute '" + attributeName + "' required? (yes/no): ");
            String isRequired = console.readLine();
            if (isRequired.equalsIgnoreCase("yes")) {
                requiredArray.put(attributeName);
            }
        }
        schemaJson.put("properties", propertiesJson);
        if (requiredArray.length() > 0) {
            schemaJson.put("required", requiredArray);
        }
        return schemaJson.toString();
    }

    private static void createDatabase(String dbName, String schema,String node) {
        HttpHeaders headers = createHeaders();
        RestTemplate restTemplate = new RestTemplate();
        JSONObject requestBodyJson = new JSONObject();
        requestBodyJson.put("dbName", dbName);
        requestBodyJson.put("schema", schema);

        HttpEntity<String> entity = new HttpEntity<>(requestBodyJson.toString(), headers);

       ResponseEntity<String> response = restTemplate.exchange("http://" + node + ":8080/database/create", HttpMethod.POST, entity, String.class);
        System.out.println(response.getBody());
    }

    public static String getAssignedNode(String username){
        String node= jedisUserNode.get(username);
        if( node!= null) return node;

        String loadBalancerURI = "http://LoadBalancer:8099/get-user-node";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(username, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(loadBalancerURI, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("from load balancer");
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to get node. Reason: " + response.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while calling the API: " + e.getMessage());
        }

    }

    private static void registerUser(User user) {
        String url = "http://BootstrapingNode:8091/create-user";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = createHeaders();

        HttpEntity<User> requestEntity = new HttpEntity<>(user, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode() == HttpStatus.CREATED) {
                System.out.println("Successfully created user. Node name: " + response.getBody());
                jedisUserNode.set(user.getUsername(), response.getBody());

            } else {
                System.out.println("Failed to create user. Reason: " + response.getBody());
            }
        } catch (Exception e) {
            System.out.println("An error occurred while calling the API: " + e.getMessage());
        }
    }


    private static HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    private static boolean checkUserPassword(User user, String node) {
        String url = "http://" + node + ":8080/login";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<User> requestEntity = new HttpEntity<>(user, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println(response.getBody());
                return true;
            }
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
            return false;
        }
        return false;
    }

    private static boolean checkUserExistence(String username) {
        return UsersMySQLUtils.isExistsUser(username);
    }
}




