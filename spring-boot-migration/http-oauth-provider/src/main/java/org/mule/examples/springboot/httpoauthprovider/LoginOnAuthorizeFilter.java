package org.mule.examples.springboot.httpoauthprovider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Mule's provider takes the login form post on the authorization URL itself and answers it with the
 * redirect to the client's redirect_uri. Here the credentials are checked against the resource owner
 * authentication manager ({@code resourceOwnerSecurityProvider}) and the request then continues to
 * Spring Authorization Server's authorization endpoint as the original GET authorization request.
 */
class LoginOnAuthorizeFilter extends OncePerRequestFilter {

    private final String authorizationPath;
    private final AuthenticationManager resourceOwners;
    private final String providerName;

    LoginOnAuthorizeFilter(String authorizationPath, AuthenticationManager resourceOwners, String providerName) {
        this.authorizationPath = authorizationPath;
        this.resourceOwners = resourceOwners;
        this.providerName = providerName;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && authorizationPath.equals(request.getRequestURI())
                && request.getParameter("username") != null);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication owner;
        try {
            owner = resourceOwners.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                    request.getParameter("username"), request.getParameter("password")));
        } catch (AuthenticationException e) {
            LoginPage.render(request, response, providerName, HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(owner);
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getMethod() {
                return "GET";
            }
        }, response);
    }
}
