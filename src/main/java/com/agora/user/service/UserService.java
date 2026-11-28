package com.agora.user.service;

import com.agora.auth.model.Role;
import com.agora.auth.model.RoleEnum;
import com.agora.user.dto.UpdateLocationRequest;
import com.agora.user.dto.UserCardDTO;
import com.agora.user.dto.UpdateProfileRequest;
import com.agora.user.repository.RoleRepository;
import com.agora.user.repository.UserRepository;
import com.agora.user.repository.UserReportRepository;
import com.agora.user.model.UserReport;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.agora.user.model.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    /** Denunciantes distintos necesarios para deshabilitar una cuenta automáticamente. */
    public static final int REPORTS_TO_DISABLE = 5;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserReportRepository userReportRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       UserReportRepository userReportRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userReportRepository = userReportRepository;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
    }

    @Transactional
    public void updateUserLocation(UpdateLocationRequest locationRequest) {
        User currentUser = getCurrentUser();
        currentUser.setLatitud(locationRequest.getLatitud());
        currentUser.setLongitud(locationRequest.getLongitud());
        userRepository.save(currentUser);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User createUser(String username, String email, String password, Set<RoleEnum> roles) {
        Set<Role> roleEntities = new HashSet<>(roleRepository.findRolesByRoleNameIn(new ArrayList<>(roles)));

        if (roleEntities.isEmpty()) {
            throw new IllegalArgumentException("Roles not found");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .roles(roleEntities)
                .accountNoExpired(true)
                .accountNoLocked(true)
                .credentialNoExpired(true)
                .isEnabled(true)
                .build();

        return userRepository.save(user);
    }


    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsEnabled(false);
        userRepository.save(user);
    }

    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    public List<UserCardDTO> getAllUserCards() {
        String currentUsername="manolo";

        return userRepository.findAll().stream()
                .filter(u -> !u.getUsername().equals(currentUsername))
                .map(u -> UserCardDTO.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .imageUrl(u.getImageUrl())
                        .historialEducativo(u.getHistorialEducativo())
                        .motivaciones(u.getMotivaciones())
                        .actividadesPersonales(u.getActividadesPersonales())
                        .proyectosRecientes(u.getProyectosRecientes())
                        .build())
                .collect(Collectors.toList());
    }


    public User updateUserImageUrl(Long userId, String imageUrl) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado con id: " + userId);
        }

        User user = optionalUser.get();
        user.setImageUrl(imageUrl);
        return userRepository.save(user);
    }
    @Transactional
    public User updateUser(UpdateProfileRequest request) {

        User currentUser = getCurrentUser();
        if (request.getUsername() != null) {
            currentUser.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            currentUser.setEmail(request.getEmail());
        }
        if (request.getCiudad() != null) {
            currentUser.setCiudad(request.getCiudad());
        }
        if (request.getPais() != null) {
            currentUser.setPais(request.getPais());
        }
        if (request.getMotivaciones() != null) {
            currentUser.setMotivaciones(String.join("\n", request.getMotivaciones()));
        }
        if (request.getActividadesPersonales() != null) {
            currentUser.setActividadesPersonales(String.join("\n", request.getActividadesPersonales()));
        }
        if (request.getProyectosRecientes() != null) {
            currentUser.setProyectosRecientes(String.join("\n", request.getProyectosRecientes()));
        }
        return userRepository.save(currentUser);
    }
    /**
     * Registra un reporte de {@code reporterId} contra {@code reportedId}.
     * Cada persona cuenta una sola vez por usuario reportado: antes un mismo usuario
     * podía reportar 5 veces y deshabilitar a cualquiera. Al llegar a
     * {@link #REPORTS_TO_DISABLE} denunciantes distintos la cuenta se deshabilita
     * (salvo administradores, que solo se marcan para revisión).
     *
     * @return true si el reporte es nuevo; false si esa persona ya lo había reportado
     */
    @Transactional
    public boolean reportUser(Long reportedId, Long reporterId) {
        if (reportedId.equals(reporterId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot report yourself");
        }
        User user = userRepository.findById(reportedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (userReportRepository.existsByReporterIdAndReportedId(reporterId, reportedId)) {
            return false;
        }
        // La restricción única (reporter_id, reported_id) cubre el caso de dos peticiones simultáneas
        userReportRepository.saveAndFlush(UserReport.builder()
                .reporterId(reporterId)
                .reportedId(reportedId)
                .createdAt(Instant.now())
                .build());

        int distinctReporters = (int) userReportRepository.countByReportedId(reportedId);
        user.setReportCount(distinctReporters);

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getRoleName() == RoleEnum.ADMIN);
        if (distinctReporters >= REPORTS_TO_DISABLE && !isAdmin) {
            user.setIsEnabled(false);
        }

        userRepository.save(user);
        return true;
    }

}
