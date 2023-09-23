package com.atypon.bootstrapingnode.services;

import com.atypon.bootstrapingnode.models.User;
import com.atypon.bootstrapingnode.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public void addUser(User user) {
        user.setNode(getAssignedNode(user.getUsername()));
        userRepository.save(user);
    }

    public String getAssignedNode(String username) {

        String loadBalancerURI = "http://LoadBalancer:8099/get-user-node";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(username, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(loadBalancerURI, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to get node. Reason: " + response.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while calling the API: " + e.getMessage());
        }

    }

    public boolean isExistsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
