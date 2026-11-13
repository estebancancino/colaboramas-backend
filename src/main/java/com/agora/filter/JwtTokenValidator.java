package com.agora.filter;

import com.agora.util.JwtUtils;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Autentica la petición a partir del JWT del header Authorization.
 *
 * Si el token no es válido (firma, emisor, vencido o sin exp) o la cuenta está
 * deshabilitada/bloqueada, la petición sigue SIN autenticar: los endpoints
 * protegidos responden 403 y los públicos (login, registro, eventos) funcionan
 * aunque el cliente envíe un token viejo. Nunca se registra el token.
 */
public class JwtTokenValidator extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JwtTokenValidator(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                DecodedJWT decodedJWT = jwtUtils.validateToken(header.substring(7));
                UserDetails userDetails = userDetailsService.loadUserByUsername(jwtUtils.extractUsername(decodedJWT));

                if (isAccountActive(userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                } else {
                    log.debug("JWT rechazado: cuenta deshabilitada o bloqueada");
                    SecurityContextHolder.clearContext();
                }
            } catch (JWTVerificationException e) {
                log.debug("JWT rechazado: {}", e.getClass().getSimpleName());
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                // Usuario inexistente, flags nulos, etc.: se trata como no autenticado
                log.debug("JWT rechazado: {}", e.getClass().getSimpleName());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isAccountActive(UserDetails user) {
        return user.isEnabled()
                && user.isAccountNonLocked()
                && user.isAccountNonExpired()
                && user.isCredentialsNonExpired();
    }
}
