package com.atithi.aavas.implementations;

import com.atithi.aavas.enums.HostelStatus;
import com.atithi.aavas.enums.SubscriptionStatus;
import com.atithi.aavas.enums.UserStatus;
import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.SubscriptionRequest;
import com.atithi.aavas.models.User;
import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.repositories.HostelRepository;
import com.atithi.aavas.repositories.SubscriptionRequestRepository;
import com.atithi.aavas.repositories.UserRepository;
import com.atithi.aavas.requests.SubmitUtrRequest;
import com.atithi.aavas.responses.SubscriptionResponse;
import com.atithi.aavas.services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRequestRepository subscriptionRequestRepository;
    private final UserRepository userRepository;
    private final HostelRepository hostelRepository;

    @Override
    @Transactional
    public SubscriptionResponse submitUtr(UserPrincipal userPrincipal, SubmitUtrRequest request) {
        if (userPrincipal == null) {
            throw new IllegalArgumentException("User is not authenticated");
        }

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userPrincipal.getId()));

        Hostel hostel = user.getHostel();
        if (hostel == null) {
            throw new IllegalArgumentException("User is not associated with any hostel");
        }

        // Check if a pending request exists for this hostel; if so, update it
        Optional<SubscriptionRequest> existingPendingOpt = subscriptionRequestRepository
                .findTopByHostel_IdOrderByCreatedAtDesc(hostel.getId())
                .filter(req -> req.getStatus() == SubscriptionStatus.PENDING);

        SubscriptionRequest subscriptionRequest;
        if (existingPendingOpt.isPresent()) {
            subscriptionRequest = existingPendingOpt.get();
            subscriptionRequest.setPlanName(request.planName());
            subscriptionRequest.setAmount(request.amount());
            subscriptionRequest.setUtrNumber(request.utrNumber().trim());
            subscriptionRequest.setNotes(request.notes());
            log.info("Updated existing pending subscription request [{}] with new UTR [{}] for hostel [{}]",
                    subscriptionRequest.getId(), request.utrNumber(), hostel.getId());
        } else {
            subscriptionRequest = SubscriptionRequest.builder()
                    .hostel(hostel)
                    .user(user)
                    .planName(request.planName())
                    .amount(request.amount())
                    .utrNumber(request.utrNumber().trim())
                    .status(SubscriptionStatus.PENDING)
                    .notes(request.notes())
                    .build();
            log.info("Created new subscription request with UTR [{}] for hostel [{}]",
                    request.utrNumber(), hostel.getId());
        }

        SubscriptionRequest saved = subscriptionRequestRepository.save(subscriptionRequest);
        return SubscriptionResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getLatestStatus(UserPrincipal userPrincipal) {
        if (userPrincipal == null || userPrincipal.getHostelId() == null) {
            return null;
        }

        return subscriptionRequestRepository
                .findTopByHostel_IdOrderByCreatedAtDesc(userPrincipal.getHostelId())
                .map(SubscriptionResponse::fromEntity)
                .orElse(null);
    }

    @Override
    @Transactional
    public void approveSubscription(Long requestId) {
        SubscriptionRequest request = subscriptionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription request not found: " + requestId));

        request.setStatus(SubscriptionStatus.APPROVED);
        subscriptionRequestRepository.save(request);

        Hostel hostel = request.getHostel();
        if (hostel != null) {
            hostel.setStatus(HostelStatus.ACTIVE);
            hostelRepository.save(hostel);
        }

        User user = request.getUser();
        if (user != null) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }

        log.info("Approved subscription request [{}]. User [{}] and Hostel [{}] activated successfully.",
                requestId, user != null ? user.getEmail() : "N/A", hostel != null ? hostel.getId() : "N/A");
    }
}
