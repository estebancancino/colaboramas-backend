package com.agora.search.dto;

import com.agora.util.ImageUrlSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileResponse {
    private Long id;
    private String nombre;
    @JsonSerialize(using = ImageUrlSerializer.class)
    private String imageUrl;
    private String pais;
    private String ciudad;
    private String profesion;
    private Set<String> camposInvestigacion;
    private Set<String> lineasInteres;
}