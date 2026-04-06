package com.habitspark.dao.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("point_account")
public class PointAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Integer totalPoints;
    private Integer availablePoints;
    private Integer frozenPoints;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
