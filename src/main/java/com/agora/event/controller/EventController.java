package com.agora.event.controller;

import com.agora.auth.service.CurrentUserService;
import com.agora.event.dto.EventDTO;
import com.agora.event.dto.CreateEventRequest;
import com.agora.event.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private CurrentUserService currentUser;

    @GetMapping
    public ResponseEntity<List<EventDTO>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PostMapping
    public ResponseEntity<EventDTO> createEvent(@RequestBody CreateEventRequest request) {
        EventDTO created = eventService.createEvent(request, currentUser.get());
        return ResponseEntity.created(java.net.URI.create("/events/" + created.getId())).body(created);
    }
}
