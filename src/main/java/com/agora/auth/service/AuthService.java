package com.agora.auth.service;

import com.agora.auth.dto.AuthCreateUserRequest;
import com.agora.auth.dto.AuthLoginRequest;
import com.agora.auth.dto.AuthResponse;

import com.agora.user.repository.RoleRepository;
import com.agora.user.repository.UserRepository;
import com.agora.auth.model.Role;
import com.agora.auth.model.RoleEnum;
import com.agora.user.model.User;
import com.agora.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private static final Set<RoleEnum> SELF_REGISTRATION_ROLES = EnumSet.of(RoleEnum.ACADEMICO, RoleEnum.COMUNICADOR);

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;


    public AuthResponse loginUser(AuthLoginRequest request) {
        String email = request.email();
        String password = request.password();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(email, password);
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtUtils.createToken(authentication);
        return new AuthResponse(email, "User logged successfully", token, true);
    }

    public AuthResponse createUser(AuthCreateUserRequest request) {
        List<RoleEnum> roleEnums = resolveSelfRegistrationRoles(request);

        Set<Role> roles = new HashSet<>(roleRepository.findRolesByRoleNameIn(roleEnums));

        if (roles.isEmpty()) {
            throw new IllegalArgumentException("The specified role does not exist");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(roles)
                .accountNoExpired(true)
                .accountNoLocked(true)
                .credentialNoExpired(true)
                .isEnabled(true)
                .build();

        userRepository.save(user);
        ArrayList<SimpleGrantedAuthority> authorityList = new ArrayList<>();
        user.getRoles().forEach(
                role -> authorityList.add(new SimpleGrantedAuthority("ROLE_".concat(role.getRoleName().name()))));
        user.getRoles()
                .stream()
                .flatMap(role -> role.getPermissionEntities().stream())
                .forEach(permission -> authorityList
                        .add(new SimpleGrantedAuthority(permission.getPermissionName().name())));
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword(),
                authorityList);

        String token = jwtUtils.createToken(authentication);

        return new AuthResponse(user.getEmail(), "User created successfully", token, true);
    }

    /**
     * El registro público solo puede asignar roles de usuario normal. ADMIN se asigna
     * únicamente desde la administración (PATCH /admin/users/{id}/role).
     */
    private List<RoleEnum> resolveSelfRegistrationRoles(AuthCreateUserRequest request) {
        List<String> requested = request.roleRequest() == null ? null : request.roleRequest().roleListName();
        if (requested == null || requested.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A role is required");
        }

        List<RoleEnum> roles = new ArrayList<>();
        for (String name : requested) {
            RoleEnum role;
            try {
                role = RoleEnum.valueOf(name.trim().toUpperCase());
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + name);
            }
            if (!SELF_REGISTRATION_ROLES.contains(role)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not allowed for registration: " + role);
            }
            roles.add(role);
        }
        return roles;
    }


}
