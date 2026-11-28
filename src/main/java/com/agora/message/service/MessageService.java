package com.agora.message.service;

import com.agora.message.dto.MessageRequestDto;
import com.agora.message.dto.MessageResponseDto;
import com.agora.message.model.Message;
import com.agora.message.repository.MessageRepository;
import com.agora.user.dto.UserSummaryDTO;
import com.agora.user.model.User;
import com.agora.user.repository.UserRepository;
import com.agora.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository repo;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;

    public MessageResponseDto sendMessage(MessageRequestDto dto) {
        if (!userRepository.existsById(dto.getToUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found");
        }

        // Guardar mensaje en BD
        Message m = new Message();
        m.setFromUserId(dto.getFromUserId());
        m.setToUserId(dto.getToUserId());
        m.setSubject(dto.getSubject());
        m.setBody(dto.getMessage());
        m.setStatus("pending");
        m.setCreatedAt(Instant.now());
        m.setUpdatedAt(Instant.now());

        Message saved = repo.save(m);

        // Enviar correo (opcional)
        sendEmailNotification(saved);

        // Crear DTO para respuesta
        MessageResponseDto response = toResponseDto(saved);

        // 🔹 Enviar notificación push al destinatario
        User senderUser = userRepository.findById(dto.getFromUserId()).orElse(null);
        String senderName = senderUser != null ? senderUser.getUsername() : "Someone";
        pushNotificationService.sendNotificationToUser(
            dto.getToUserId(),
            "Nuevo mensaje de " + senderName,
            dto.getSubject()
        );
        logger.info("📱 Push notification sent to user: {} about new message", dto.getToUserId());

        return response;
    }

    public List<MessageResponseDto> getReceived(Long userId) {
        return repo.findByToUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponseDto).toList();
    }

    public List<MessageResponseDto> getSent(Long userId) {
        return repo.findByFromUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponseDto).toList();
    }

    /** Solo el destinatario del mensaje puede aceptarlo o rechazarlo. */
    public MessageResponseDto respond(Long id, String status, Long responderId) {
        if (!status.equals("accepted") && !status.equals("rejected") && !status.equals("pending")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }

        Message m = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));
        if (!m.getToUserId().equals(responderId)) {
            // 404 en vez de 403 para no revelar que el mensaje existe
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado");
        }

        m.setStatus(status);
        m.setUpdatedAt(Instant.now());

        Message saved = repo.save(m);

        // 🔹 Opcional: enviar notificación en tiempo real del cambio de estado
       // messagingTemplate.convertAndSend("/topic/messages/" + saved.getToUserId(), toResponseDto(saved));
       if ("accepted".equals(status)) {
        User responder = userRepository.findById(saved.getToUserId()).orElse(null);
        String responderName = responder != null ? responder.getUsername() : "A user";


        pushNotificationService.sendNotificationToUser(
            saved.getFromUserId(), // Notify the original sender
            "Conexión aceptada!",
            responderName + " aceptó tu solicitud."
        );
        logger.info("📱 Acceptance notification sent to user: {}", saved.getFromUserId());
    }

        return toResponseDto(saved);
    }

    private void sendEmailNotification(Message m) {
        User remitente = userRepository.findById(m.getFromUserId()).orElse(null);
        User destinatario = userRepository.findById(m.getToUserId()).orElse(null);

        if (destinatario != null && destinatario.getEmail() != null) {
            String logoUrl = "https://logowik.com/content/uploads/images/agora7391.logowik.com.webp";

            // Todo lo que escribe un usuario se escapa: sin esto el remitente podía inyectar
            // HTML (enlaces, botones falsos) en un correo enviado desde la cuenta de Agora.
            String remitenteNombre = HtmlUtils.htmlEscape(remitente != null ? remitente.getUsername() : "Usuario");
            String asunto = HtmlUtils.htmlEscape(m.getSubject());
            String cuerpo = HtmlUtils.htmlEscape(m.getBody()).replace("\n", "<br/>");

            // Sin botones de Aceptar/Rechazar: apuntaban a miapp.com y responder exige sesión
            // (PUT autenticado), así que la respuesta se hace desde la app.
            String html = "<html>"
                    + "<body style='font-family:Arial,sans-serif;color:#333;'>"
                    + "<div style='text-align:center;'>"
                    + "<img src='" + logoUrl + "' alt='Agora' width='150' style='margin-bottom:20px;'/>"
                    + "<h2>¡Has recibido un mensaje en Agora!</h2>"
                    + "<p>De: <strong>" + remitenteNombre + "</strong></p>"
                    + "<p>Asunto: <strong>" + asunto + "</strong></p>"
                    + "<p>Mensaje: " + cuerpo + "</p>"
                    + "<p style='margin-top:30px;'>Abre la app Agora, en la sección Contactos, para aceptar o rechazar la solicitud.</p>"
                    + "</div>"
                    + "</body>"
                    + "</html>";

            emailService.enviarCorreo(destinatario.getEmail(),
                    "Alguien te ha contactado en Agora",
                    html);
        }
    }

    private MessageResponseDto toResponseDto(Message m) {
        MessageResponseDto r = new MessageResponseDto();
        r.setId(m.getId());
        r.setFromUserId(m.getFromUserId());
        r.setToUserId(m.getToUserId());
        r.setSubject(m.getSubject());
        r.setMessage(m.getBody());
        r.setStatus(m.getStatus());
        r.setCreatedAt(m.getCreatedAt());

        // Información remitente
        User remitente = userRepository.findById(m.getFromUserId()).orElse(null);
        if (remitente != null) {
            r.setFromUser(new UserSummaryDTO(
                    remitente.getId(),
                    remitente.getUsername(),
                    remitente.getEmail(),
                    remitente.getImageUrl()
            ));
        }

        // Información destinatario
        User destinatario = userRepository.findById(m.getToUserId()).orElse(null);
        if (destinatario != null) {
            r.setToUser(new UserSummaryDTO(
                    destinatario.getId(),
                    destinatario.getUsername(),
                    destinatario.getEmail(),
                    destinatario.getImageUrl()
            ));
        }

        return r;
    }
}
