package com.agora.user.model;

import com.agora.util.ImageUrlSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.agora.profile.model.Educacion;
import com.agora.auth.model.Role;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name="image_url",length=1000)
    @JsonSerialize(using = ImageUrlSerializer.class)
    private String imageUrl;

    @Column(nullable = false)
    private String password;

    @Column(name = "is_enabled")
    private Boolean isEnabled;

    @Column(name = "account_no_expired")
    private Boolean accountNoExpired;

    @Column(name = "account_no_locked")
    private Boolean accountNoLocked;

    @Column(name = "credential_no_expired")
    private Boolean credentialNoExpired;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    @Builder.Default
    private Set<Educacion> historialEducativo = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonManagedReference(value = "user-keywords")
    private Set<UserKeyword> keywords = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String motivaciones;

    @Column(columnDefinition = "TEXT")
    private String actividadesPersonales;

    @Column(columnDefinition = "TEXT")
    private String proyectosRecientes;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column
    private String pais;

    @Column
    private String ciudad;

    @Column()
    private Double latitud;

    @Column()
    private Double longitud;

    @Column(name = "report_count", nullable = false)
    @Builder.Default
    private Integer reportCount = 0;

    public void addEducacion(Educacion educacion) {
        if (this.historialEducativo == null) {
            this.historialEducativo = new HashSet<>();
        }
        this.historialEducativo.add(educacion);
        educacion.setUser(this);
    }

    public void addKeyword(UserKeyword userKeyword) {
        if (this.keywords == null) {
            this.keywords = new HashSet<>();
        }
        this.keywords.add(userKeyword);
        userKeyword.setUser(this);
    }
}
