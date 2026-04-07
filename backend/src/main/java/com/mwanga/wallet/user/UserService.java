package com.mwanga.wallet.user;

import com.mwanga.wallet.common.PageResponse;
import com.mwanga.wallet.common.exception.ResourceNotFoundException;
import com.mwanga.wallet.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        return PageResponse.from(
                userRepository.findAllByOrderByCreatedAtDesc(pageable)
                        .map(UserResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(User principal) {
        // Re-fetch to guarantee freshness; principal may be from a cached auth token
        return getUserById(principal.getId());
    }

    @Transactional
    public UserResponse setUserActive(UUID id, boolean active) {
        User user = findOrThrow(id);
        user.setActive(active);
        User saved = userRepository.save(user);
        log.info("User {} {} ", id, active ? "activated" : "deactivated");
        return UserResponse.from(saved);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", "id", id));
    }
}
