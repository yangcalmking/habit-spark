package com.habitspark.dao.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long familyGroupId;
    private String username;
    private String passwordHash;
    private String role; // student/parent/admin
    private String displayName;
    private String avatarUrl;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
