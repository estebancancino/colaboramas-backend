package com.agora.user.dto;

import com.agora.util.ImageUrlSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.agora.auth.model.Role;
import com.agora.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    @JsonSerialize(using = ImageUrlSerializer.class)
    private String imageUrl;
    private String ciudad;
    private String pais;
    private String motivaciones;
    private String actividadesPersonales;
    private String proyectosRecientes;
    private Set<String> roles;
    private Integer reportCount;


    public static UserResponseDTO fromEntity(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .ciudad(user.getCiudad())
                .pais(user.getPais())
                .motivaciones(user.getMotivaciones())
                .actividadesPersonales(user.getActividadesPersonales())
                .proyectosRecientes(user.getProyectosRecientes())
                .roles(user.getRoles().stream()
                        .map(role -> role.getRoleName().name())
                        .collect(Collectors.toSet()))
                .reportCount(user.getReportCount())
                .build();
    }
}
