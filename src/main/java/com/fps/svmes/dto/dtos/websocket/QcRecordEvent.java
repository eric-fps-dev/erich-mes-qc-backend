package com.fps.svmes.dto.dtos.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QcRecordEvent {
    private Long templateId;
    private String collectionName;
}
