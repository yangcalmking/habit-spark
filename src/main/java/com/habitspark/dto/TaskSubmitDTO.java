package com.habitspark.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskSubmitDTO {
    @NotNull(message = "任务模板ID不能为空")
    private Long taskTemplateId;

    private String description; // 文字说明
    private String attachmentUrl; // 图片凭证URL
}
