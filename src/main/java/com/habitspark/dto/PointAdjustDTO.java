package com.habitspark.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PointAdjustDTO {
    @NotNull(message = "积分值不能为空")
    private Integer points; // 正数=加分, 负数=扣分

    @NotBlank(message = "原因不能为空")
    private String reason;

    @NotNull(message = "学生ID不能为空")
    private Long studentId;
}
