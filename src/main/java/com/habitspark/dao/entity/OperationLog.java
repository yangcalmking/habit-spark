package com.habitspark.dao.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String operationType;
    private String targetEntity;
    private Long targetId;
    private String description;
    private String details;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
