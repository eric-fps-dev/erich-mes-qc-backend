package com.fps.svmes.models.sql.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.models.sql.Common;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "form_notification_config", schema = "quality_management")
@Data
@EqualsAndHashCode(callSuper = true)
public class FormNotificationConfig extends Common {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonProperty("form_template_id")
    @Column(name = "form_template_id", nullable = false, unique = true)
    private Long formTemplateId;

    @JsonProperty("trigger_type")
    @Column(name = "trigger_type", nullable = false)
    private String triggerType = "always";

    @JsonProperty("delivery_method")
    @Column(name = "delivery_method", nullable = false)
    private String deliveryMethod = "email";
}
