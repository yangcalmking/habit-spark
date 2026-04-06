package com.habitspark.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 30, message = "用户名长度 4-30 字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度 6-64 字符")
    private String password;

    @NotBlank(message = "显示名不能为空")
    private String displayName;

    @NotNull(message = "角色不能为空")
    private String role; // student/parent

    private Long familyGroupId; // 如果加入已有家庭组
}
