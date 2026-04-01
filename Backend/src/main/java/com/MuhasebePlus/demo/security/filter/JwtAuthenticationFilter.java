package com.MuhasebePlus.demo.security.filter;

import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // JWT doğrulama işlemleri burada yapılacaktır.
   @Override
        protected void doFilterInternal(HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain)
        throws ServletException, IOException {
            filterChain.doFilter(request, response);
}
}
