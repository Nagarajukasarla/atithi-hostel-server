package com.atithi.aavas.security;

import com.atithi.aavas.enums.HostelStatus;
import com.atithi.aavas.enums.UserStatus;
import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.User;
import com.atithi.aavas.models.UserPrincipal;
import com.atithi.aavas.repositories.HostelRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActiveAccountFilter extends OncePerRequestFilter {

    private final HostelRepository hostelRepository;

    private static final List<String> WHITELISTED_PREFIXES = List.of(
            "/api/v1/auth/",
            "/api/public/",
            "/api/v1/public/",
            "/api/v1/subscription/",
            "/actuator",
            "/health",
            "/error"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. If path is whitelisted (auth, subscription, public, error), allow it through
        for (String prefix : WHITELISTED_PREFIXES) {
            if (path.startsWith(prefix)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // 2. Check if the user is authenticated and verify account & hostel active status
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            User user = userPrincipal.getUser();

            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                log.warn("Blocked inactive user [{}] from accessing protected path [{}]",
                        user != null ? user.getEmail() : "unknown", path);
                sendForbiddenResponse(response, "Access Denied: Your account is pending plan activation. Access to hostel management features is restricted.");
                return;
            }

            Long hostelId = user.getHostelId();
            if (hostelId != null) {
                HostelStatus hostelStatus;
                if (user.getHostel() != null && Hibernate.isInitialized(user.getHostel())) {
                    hostelStatus = user.getHostel().getStatus();
                } else {
                    hostelStatus = hostelRepository.findById(hostelId)
                            .map(Hostel::getStatus)
                            .orElse(null);
                }

                if (hostelStatus != null && hostelStatus != HostelStatus.ACTIVE) {
                    log.warn("Blocked user [{}] with inactive hostel [{}] from accessing protected path [{}]",
                            user.getEmail(), hostelId, path);
                    sendForbiddenResponse(response, "Access Denied: Your hostel workspace is inactive. Please complete plan activation.");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"success\":false,\"message\":\"%s\",\"data\":null,\"errorCode\":\"ACCOUNT_INACTIVE\"}",
                message
        ));
    }
}
