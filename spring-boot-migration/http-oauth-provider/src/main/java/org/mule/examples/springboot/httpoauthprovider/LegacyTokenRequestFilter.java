package org.mule.examples.springboot.httpoauthprovider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Mule's token endpoint also accepts the grant as GET query parameters, which is how the README's
 * redirectFlow reaches it (browser follows the 302 to /token?grant_type=...&client_secret=...).
 * Spring Authorization Server only reads form-encoded POSTs, so such a GET is presented to it as one.
 */
class LegacyTokenRequestFilter extends OncePerRequestFilter {

    private final String tokenPath;

    LegacyTokenRequestFilter(String tokenPath) {
        this.tokenPath = tokenPath;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("GET".equals(request.getMethod()) && tokenPath.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getMethod() {
                return "POST";
            }

            @Override
            public String getQueryString() {
                return null;
            }

            @Override
            public String getContentType() {
                return MediaType.APPLICATION_FORM_URLENCODED_VALUE;
            }
        }, response);
    }
}
