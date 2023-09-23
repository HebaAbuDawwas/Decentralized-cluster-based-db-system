package com.atypon.nosqlstoragenode.loginhandler.controller;

import com.atypon.nosqlstoragenode.loginhandler.models.User;
import com.atypon.nosqlstoragenode.loginhandler.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User loginUser) {
        String username = loginUser.getUsername();
        String password = loginUser.getPassword();

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            if (password.equals(optionalUser.get().getPassword())) {
                return new ResponseEntity<>("Login successful", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Invalid password", HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>("User does not exists", HttpStatus.BAD_REQUEST);
        }
    }


}
