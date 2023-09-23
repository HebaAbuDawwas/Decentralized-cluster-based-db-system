package com.atypon.bootstrapingnode.repositories;

import com.atypon.bootstrapingnode.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUsername(String username);
}
