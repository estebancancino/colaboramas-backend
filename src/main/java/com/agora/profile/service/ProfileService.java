package com.agora.profile.service;

import com.agora.profile.dto.EducacionDto;
import com.agora.profile.dto.KeywordDto;
import com.agora.profile.dto.UpdateProfileRequest;
import com.agora.profile.dto.UserProfileResponseDto;
import com.agora.profile.model.Educacion;
import com.agora.tag.repository.KeywordRepository;
import com.agora.user.repository.UserRepository;
import com.agora.tag.model.Keyword;
import com.agora.user.model.User;
import com.agora.user.model.UserKeyword;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService implements IProfileService {

    private final UserRepository userRepository;
    private final KeywordRepository keywordRepository;

    @Override
    @Transactional
    public UserProfileResponseDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);

        user.setMotivaciones(request.getMotivaciones());
        user.setActividadesPersonales(request.getActividadesPersonales());
        user.setProyectosRecientes(request.getProyectosRecientes());

        user.setPais(request.getPais());
        user.setCiudad(request.getCiudad());
        user.setLatitud(request.getLatitud());
        user.setLongitud(request.getLongitud());
        // Actualizar historial educativo
        if (request.getHistorialEducativo() != null) {
            user.getHistorialEducativo().clear();
            Set<Educacion> nuevoHistorial = request.getHistorialEducativo().stream()
                    .map(dto -> {
                        Educacion edu = new Educacion();
                        edu.setInstitucion(dto.getInstitucion());
                        edu.setTitulo(dto.getTitulo());
                        edu.setUser(user);
                        return edu;
                    }).collect(Collectors.toSet());
            user.getHistorialEducativo().addAll(nuevoHistorial);
        }

        // Actualizar keywords
        if (request.getKeywords() != null) {

            user.getKeywords().clear();

            Set<UserKeyword> newUserKeywords = new HashSet<>();
            for (var keywordDto : request.getKeywords()) {
                // Buscar si el keyword ya existe, si no, crearlo.
                Keyword keyword = keywordRepository.findByName(keywordDto.getName())
                        .orElseGet(() -> {
                            Keyword newKeyword = new Keyword();
                            newKeyword.setName(keywordDto.getName());
                            return keywordRepository.save(newKeyword);
                        });

                UserKeyword userKeyword = new UserKeyword();
                userKeyword.setUser(user);
                userKeyword.setKeyword(keyword);
                userKeyword.setType(keywordDto.getType());
                newUserKeywords.add(userKeyword);
            }
            user.getKeywords().addAll(newUserKeywords);
        }

        User updatedUser = userRepository.save(user);

        return toUserProfileResponseDto(updatedUser);
    }

    @Override
    public UserProfileResponseDto getProfile(Long userId) {
        return toUserProfileResponseDto(findUser(userId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));
    }

    private UserProfileResponseDto toUserProfileResponseDto(User user) {
        UserProfileResponseDto dto = new UserProfileResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setMotivaciones(user.getMotivaciones());
        dto.setActividadesPersonales(user.getActividadesPersonales());
        dto.setProyectosRecientes(user.getProyectosRecientes());
        dto.setPais(user.getPais());
        dto.setCiudad(user.getCiudad());
        dto.setLatitud(user.getLatitud());
        dto.setLongitud(user.getLongitud());

        dto.setHistorialEducativo(user.getHistorialEducativo().stream()
                .map(this::toEducacionDto)
                .collect(Collectors.toList()));

        dto.setKeywords(user.getKeywords().stream()
                .map(this::toKeywordDto)
                .collect(Collectors.toList()));

        return dto;
    }

    private EducacionDto toEducacionDto(Educacion educacion) {
        EducacionDto dto = new EducacionDto();
        dto.setInstitucion(educacion.getInstitucion());
        dto.setTitulo(educacion.getTitulo());
        return dto;
    }

    private KeywordDto toKeywordDto(UserKeyword userKeyword) {
        KeywordDto dto = new KeywordDto();
        dto.setName(userKeyword.getKeyword().getName());
        dto.setType(userKeyword.getType());
        return dto;
    }
}
