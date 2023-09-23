package com.atypon.userinteractiveservice.utils.usersmysql.repositories;


import com.atypon.userinteractiveservice.utils.usersmysql.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    Boolean existsByUsername(String username);
}
