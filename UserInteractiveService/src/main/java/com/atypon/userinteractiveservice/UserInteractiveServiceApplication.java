package com.atypon.userinteractiveservice;

import com.atypon.userinteractiveservice.utils.usersmysql.UsersMySQLUtils;
import com.atypon.userinteractiveservice.utils.usersmysql.models.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.Jedis;

import java.io.Console;


@SpringBootApplication
public class UserInteractiveServiceApplication {
    static Jedis jedisUserNode = new Jedis("redis://redis:6379");


    public static void main(String[] args) {

        SpringApplication.run(UserInteractiveServiceApplication.class, args);
        //docker attach UserInteractionService
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

            String query = console.readLine("> ");
            if ("exit".equalsIgnoreCase(query)) {
                break;
            }
            executeQuery(query, assignedNode);
        }
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

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

    private static void executeQuery(String query, String node) {
        String url = "http://" + node + ":8080/pass-query";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(query, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            System.out.println(response.getBody());
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
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




