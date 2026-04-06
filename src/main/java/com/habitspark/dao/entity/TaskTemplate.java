package com.habitspark.dao.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("task_template")
public class TaskTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private Integer basePoints;
    private String extraPointsRule;
    private Integer dailyCap;
    private String description;
    private String standard;
    private Integer sortOrder;
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
