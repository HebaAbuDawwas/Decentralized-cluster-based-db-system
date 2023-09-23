package com.atypon.loadpalncer.usersbalancer;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserBalancerController {

    @Autowired
    private UsersConsistentHashing userConsistentHashing;

    @PostMapping("/get-user-node")
    public ResponseEntity<String> getNodeForUser(@RequestBody String username) {
        try {
            String nodeName = userConsistentHashing.get(username);
            if (nodeName != null) {
                return new ResponseEntity<>(nodeName, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("No node found for  user: " + username, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
