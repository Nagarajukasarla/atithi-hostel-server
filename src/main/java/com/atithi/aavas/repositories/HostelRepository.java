package com.atithi.aavas.repositories;

import com.atithi.aavas.models.Hostel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HostelRepository extends JpaRepository<Hostel, Long> {
    Optional<Hostel> findByToken(String token);
    boolean existsByToken(String token);
}
