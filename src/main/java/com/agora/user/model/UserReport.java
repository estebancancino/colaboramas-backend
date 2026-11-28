package com.agora.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Un reporte de un usuario sobre otro. La restricción única impide que la misma
 * persona sume varios reportes contra el mismo usuario.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_reports",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_reports_reporter_reported",
                columnNames = {"reporter_id", "reported_id"}))
public class UserReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "reported_id", nullable = false)
    private Long reportedId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
