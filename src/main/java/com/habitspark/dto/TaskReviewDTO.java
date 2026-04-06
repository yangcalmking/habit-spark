package com.habitspark.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskReviewDTO {
    @NotNull(message = "任务记录ID不能为空")
    private Long taskRecordId;

    @NotNull(message = "审核结果不能为空")
    private Boolean approved; // true=通过, false=拒绝

    private String comment; // 家长评语（拒绝时必填）
}
