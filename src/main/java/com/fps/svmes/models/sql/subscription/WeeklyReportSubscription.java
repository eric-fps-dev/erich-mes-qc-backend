package com.fps.svmes.models.sql.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.models.sql.Common;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "weekly_report_subscription", schema = "quality_management")
@Data
@EqualsAndHashCode(callSuper = true)
public class WeeklyReportSubscription extends Common {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @JsonProperty("user_id")
    @Column(name = "user_id")
    private Integer userId;

    @JsonProperty("email")
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "language")
    private String language = "en";
}
