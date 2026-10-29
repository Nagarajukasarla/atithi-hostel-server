package com.atithi.aavas.repositories;

import com.atithi.aavas.models.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"hostel"})
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
