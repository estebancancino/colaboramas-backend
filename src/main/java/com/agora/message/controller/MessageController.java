package com.agora.message.controller;

import com.agora.message.model.Message;
import com.agora.message.model.UserInteraction;
import com.agora.message.repository.UserInteractionRepository;
import com.agora.message.service.EmailService;
import com.agora.message.service.UserInteractionService;
import com.agora.user.repository.UserRepository;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.agora.message.dto.MessageRequestDto;
import com.agora.message.dto.MessageResponseDto;
import com.agora.message.repository.MessageRepository;
import com.agora.message.service.MessageService;
import com.agora.auth.service.CurrentUserService;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    /** Solicitudes en curso o ya aceptadas entre dos usuarios. */
    private static final List<String> OPEN_STATUSES = List.of("pending", "accepted");
    private static final List<String> ANY_STATUS = List.of("pending", "accepted", "rejected");
    /** Máximo de solicitudes nuevas que un usuario puede enviar en 24 horas (anti-spam). */
    static final int MAX_MESSAGES_PER_DAY = 20;

    private final MessageService messageService;
    private final MessageRepository messageRepository;
    private final UserInteractionService userInteractionService;
    private final CurrentUserService currentUser;

    @PostMapping("/send")
    public ResponseEntity<MessageResponseDto> send(
            @Valid @RequestBody MessageRequestDto dto
    ) {
        // El remitente es siempre el usuario autenticado (no se puede suplantar a otro)
        Long me = currentUser.id();
        if (dto.getFromUserId() != null && !dto.getFromUserId().equals(me)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "fromUserId must be the authenticated user");
        }
        if (me.equals(dto.getToUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send a message to yourself");
        }
        dto.setFromUserId(me);

        // Una sola solicitud por destinatario: no se puede repetir aunque haya sido rechazada
        // (evita insistir/acosar). Si el otro ya me escribió y sigue pendiente o aceptada,
        // tampoco: corresponde responder esa solicitud en vez de crear otra.
        Long to = dto.getToUserId();
        if (messageRepository.existsByFromUserIdAndToUserIdAndStatusIn(me, to, ANY_STATUS)
                || messageRepository.existsByFromUserIdAndToUserIdAndStatusIn(to, me, OPEN_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A request with this user already exists");
        }
        Instant since = Instant.now().minus(Duration.ofDays(1));
        if (messageRepository.countByFromUserIdAndCreatedAtAfter(me, since) >= MAX_MESSAGES_PER_DAY) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Daily message limit reached");
        }

        MessageResponseDto res = messageService.sendMessage(dto);
        userInteractionService.registerContact(me, dto.getToUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/received/{userId}")
    public ResponseEntity<List<MessageResponseDto>> getReceived(@PathVariable Long userId) {
        currentUser.requireSelfOrAdmin(userId);
        return ResponseEntity.ok(messageService.getReceived(userId));
    }

    @GetMapping("/sent/{userId}")
    public ResponseEntity<List<MessageResponseDto>> getSent(@PathVariable Long userId) {
        currentUser.requireSelfOrAdmin(userId);
        return ResponseEntity.ok(messageService.getSent(userId));
    }

    @PutMapping("/{id}/respond")
    public ResponseEntity<?> respond(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        // Validación de estados permitidos
        if (!status.equals("pending") &&
                !status.equals("accepted") &&
                !status.equals("rejected")) {
            return ResponseEntity
                    .badRequest()
                    .body("Invalid status: " + status + " (allowed: pending, accepted, rejected)");
        }
        MessageResponseDto res = messageService.respond(id, status, currentUser.id());
        return ResponseEntity.ok(res);
    }
}
