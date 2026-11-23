package com.agora.profile.controller;

import com.agora.auth.service.CurrentUserService;
import com.agora.profile.dto.UpdateProfileRequest;
import com.agora.profile.dto.UserProfileResponseDto;
import com.agora.profile.service.IProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final IProfileService profileService;
    private final CurrentUserService currentUser;

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponseDto> updateProfile(@PathVariable Long userId, @RequestBody UpdateProfileRequest request) {
        currentUser.requireSelfOrAdmin(userId);
        UserProfileResponseDto updatedUser = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponseDto> getProfile(@PathVariable Long userId) {
        UserProfileResponseDto userProfile = profileService.getProfile(userId);
        // El correo y la ubicación exacta solo los ve el dueño del perfil (o un ADMIN)
        if (!currentUser.isSelfOrAdmin(userId)) {
            userProfile.setEmail(null);
            userProfile.setLatitud(null);
            userProfile.setLongitud(null);
        }
        return ResponseEntity.ok(userProfile);
    }
}
