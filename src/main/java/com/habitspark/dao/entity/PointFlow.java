package com.habitspark.dao.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("point_flow")
public class PointFlow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Integer points;
    private String reason;
    private String sourceType;
    private Long sourceId;
    private Long operatorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
