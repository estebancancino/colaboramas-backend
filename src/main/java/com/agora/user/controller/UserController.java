package com.agora.user.controller;

import com.agora.auth.service.CurrentUserService;
import com.agora.user.dto.CompleteProfileDTO;
import com.agora.user.dto.CreateUserRequest;
import com.agora.user.dto.UpdateProfileRequest;
import com.agora.user.dto.UserCardDTO;
import com.agora.user.dto.UserDTO;
import com.agora.user.model.User;
import com.agora.user.service.UserImageService;
import com.agora.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final UserImageService userImageService;
    private final CurrentUserService currentUser;

    public UserController(UserService userService, UserImageService userImageService, CurrentUserService currentUser) {
        this.userService = userService;
        this.userImageService = userImageService;
        this.currentUser = currentUser;
    }

    @PostMapping("/upload-image/{userId}")
    public ResponseEntity<String> uploadImage(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        // Antes de subir nada: solo el dueño (o un ADMIN) cambia la foto
        currentUser.requireSelfOrAdmin(userId);
        try {
            String imageKey = userImageService.uploadUserImage(file);
            userService.updateUserImageUrl(userId, imageKey);
            return ResponseEntity.ok(userImageService.resolveImageUrl(imageKey));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading image");
        }
    }

    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers().stream().map(u -> new UserDTO(u.getId(), u.getUsername()))
                .collect(Collectors.toList());
    }

    // Permite asignar cualquier rol (incluido ADMIN): solo administradores
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO createUser(@RequestBody CreateUserRequest request) {
        User user = userService.createUser(
                request.username(),
                request.email(),
                request.password(),
                request.roles()
        );
        return new UserDTO(user.getId(), user.getUsername());
    }
    @PutMapping("/update")
    public ResponseEntity<User> updateProfile(@RequestBody UpdateProfileRequest request) {
        User updated = userService.updateUser(request);
        return ResponseEntity.ok(updated);
    }

    /* @PutMapping("/{userId}/complete-profile")
    public ResponseEntity<UserDTO> completeProfile(
            @PathVariable Long userId,
            @RequestBody CompleteProfileDTO dto
    ) {
        User updatedUser = userService.completeUserProfile(userId, dto);
        return ResponseEntity.ok(new UserDTO(updatedUser.getId(), updatedUser.getUsername()));
    }*/

    @GetMapping("/cards")
    public List<UserCardDTO> getAllUserCards(Authentication auth) {
        return userService.getAllUserCards();
    }

    // El denunciante sale del JWT. Reportar dos veces al mismo usuario no suma (200 igual,
    // para que la app no muestre error); usuario inexistente 404, reportarse a sí mismo 400.
    @PostMapping("/{userId}/report")
    public ResponseEntity<String> reportUser(@PathVariable Long userId) {
        boolean created = userService.reportUser(userId, currentUser.id());
        return ResponseEntity.ok(created ? "Report received" : "Report already received");
    }
    // Antes comparaba el username con el email del token (siempre 403 y explotable
    // cambiando el propio email). Ahora se compara el ID del usuario autenticado.
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        currentUser.requireSelfOrAdmin(userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
