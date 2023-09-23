package com.atypon.bootstrapingnode.controllers;

import com.atypon.bootstrapingnode.models.User;
import com.atypon.bootstrapingnode.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/create-user")
    public ResponseEntity<String> createUser(@RequestBody User requestUser) {
        try {
            if (userService.isExistsByUsername(requestUser.getUsername())) {
                return new ResponseEntity<>("Username already exists!", HttpStatus.BAD_REQUEST);
            }
            userService.addUser(requestUser);
            String assignedNode = requestUser.getNode();
            return new ResponseEntity<>(assignedNode, HttpStatus.CREATED);

        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}