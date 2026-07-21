package com.MuhasebePlus.demo.common.service;

import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyContext {

    private final UserRepository userRepository;

    /**
     * Güvenlik bağlamından (Security Context) giriş yapan kullanıcıyı bulur
     * ve bağlı olduğu şirketin kimliğini (companyId) döndürür.
     * 
     * @return Long companyId
     * @throws RuntimeException Kullanıcı yetkisizse veya şirkete bağlı değilse
     */
    public Long getCurrentCompanyId() {
        User user = getCurrentUser();

        if (user.getCompany() == null) {
            throw new RuntimeException("User is not associated with any company");
        }

        return user.getCompany().getCompanyId();
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("No authenticated user found in SecurityContext");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }

        // Fallback: test profili veya anormal durum için DB lookup
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }
}
