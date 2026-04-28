package com.fps.svmes.models.sql.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Table(name = "form_notification_recipient", schema = "quality_management")
@Data
public class FormNotificationRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonProperty("config_id")
    @Column(name = "config_id", nullable = false)
    private Long configId;

    @JsonProperty("user_id")
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @JsonProperty("user_email")
    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @JsonProperty("user_name")
    @Column(name = "user_name")
    private String userName;

    @Column(name = "status")
    private Integer status = 1;

    @JsonProperty("created_at")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (status == null) status = 1;
    }
}
