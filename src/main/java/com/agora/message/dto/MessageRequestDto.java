package com.agora.message.dto;

import com.agora.user.dto.UserSummaryDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MessageRequestDto {
    // Opcional: el remitente real se toma del JWT; si viene y no coincide, se rechaza (403)
    private Long fromUserId;

    @NotNull
    private Long toUserId;

    @NotBlank
    @Size(max = 120)
    private String subject;

    @NotBlank
    @Size(max = 1000)
    private String message;



    // getters / setters
    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
