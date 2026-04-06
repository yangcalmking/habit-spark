package com.habitspark.dao.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("task_record")
public class TaskRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long taskTemplateId;
    private String description;
    private String attachmentUrl;
    private Integer points;
    private String status;
    private Long reviewerId;
    private String parentComment;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime submitTime;
    private LocalDateTime reviewTime;
}
