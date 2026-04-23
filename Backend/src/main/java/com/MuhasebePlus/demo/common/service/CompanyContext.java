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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("No authenticated user found in SecurityContext");
        }
        
        String email = authentication.getName(); // JWT token'ından çıkarılan kullanıcı adı (email)
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
                
        if (user.getCompany() == null) {
            throw new RuntimeException("User is not associated with any company");
        }
        
        return user.getCompany().getCompanyId();
    }
}
