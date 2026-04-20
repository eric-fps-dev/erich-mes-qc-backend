package com.fps.svmes.dto.requests.alert;

import lombok.Data;

@Data
public class UpdateAlertRecordRequest {
    private Long id;         // 告警记录ID
    private Integer rpn;     // 新的RPN值
    private Integer updatedBy; // 当前操作用户
}
