package com.atithi.aavas.services;

import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.requests.SubmitUtrRequest;
import com.atithi.aavas.responses.SubscriptionResponse;

public interface SubscriptionService {

    SubscriptionResponse submitUtr(UserPrincipal userPrincipal, SubmitUtrRequest request);

    SubscriptionResponse getLatestStatus(UserPrincipal userPrincipal);

    void approveSubscription(Long requestId);
}
