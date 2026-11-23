package com.agora.event.service;

import com.agora.event.dto.CreateEventRequest;
import com.agora.event.dto.EventDTO;
import com.agora.event.model.Event;
import com.agora.event.repository.EventRepository;
import com.agora.user.model.User;
import com.agora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository; // Para buscar al usuario

    /**
     * El organizador es el usuario autenticado; el userId del cuerpo se ignora
     * para que nadie publique eventos a nombre de otro.
     */
    public EventDTO createEvent(CreateEventRequest request, User organizer) {
        Event event = Event.builder()
                .title(request.getTitle())
                .type(request.getType())
                .date(request.getDate())
                .ubication(request.getUbication())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl()) // <-- asignar la imagen
                .user(organizer)
                .isEnabled(true) // antes quedaba null
                .build();

        Event savedEvent = eventRepository.save(event);
        return toDTO(savedEvent);
    }

    public List<EventDTO> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Convierte una entidad Event a un EventDTO
    public EventDTO toDTO(Event event) {
        EventDTO dto = new EventDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setType(event.getType());
        dto.setDate(event.getDate());
        dto.setUbication(event.getUbication());
        dto.setDescription(event.getDescription());
        dto.setImageUrl(event.getImageUrl());
        // If the event date is in the past, ensure it is disabled and persist the change
        Boolean enabled = event.getIsEnabled();
        if (event.getDate() != null && event.getDate().isBefore(LocalDateTime.now())) {
            if (!Boolean.FALSE.equals(enabled)) {
                // persist change to mark as disabled
                event.setIsEnabled(false);
                eventRepository.save(event);
            }
            dto.setIsEnabled(false);
        } else {
            dto.setIsEnabled(enabled);
        }
        if (event.getUser() != null) {
            dto.setUserId(event.getUser().getId());
        }
        return dto;
    }

    public EventDTO getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        return toDTO(event);
    }
}