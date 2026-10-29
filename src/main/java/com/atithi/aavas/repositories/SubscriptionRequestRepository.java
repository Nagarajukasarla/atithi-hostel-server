package com.atithi.aavas.repositories;

import com.atithi.aavas.models.SubscriptionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRequestRepository extends JpaRepository<SubscriptionRequest, Long> {

    Optional<SubscriptionRequest> findTopByHostel_IdOrderByCreatedAtDesc(Long hostelId);

    List<SubscriptionRequest> findByHostel_IdOrderByCreatedAtDesc(Long hostelId);

    boolean existsByUtrNumber(String utrNumber);
}
