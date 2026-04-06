package com.habitspark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.habitspark.dao.entity.*;
import com.habitspark.dao.mapper.*;
import com.habitspark.dto.*;
import com.habitspark.service.*;
import com.habitspark.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PointAccountMapper pointAccountMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponseDTO register(RegisterDTO dto) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(qw) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setFamilyGroupId(dto.getFamilyGroupId() != null ? dto.getFamilyGroupId() : 1L);
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getDisplayName());
        user.setRole(dto.getRole());
        userMapper.insert(user);

        // 学生用户创建积分账户
        if ("student".equals(dto.getRole())) {
            PointAccount account = new PointAccount();
            account.setStudentId(user.getId());
            account.setTotalPoints(0);
            account.setAvailablePoints(0);
            account.setFrozenPoints(0);
            pointAccountMapper.insert(account);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return AuthResponseDTO.builder()
                .accessToken(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .displayName(user.getNickname())
                .build();
    }

    @Override
    public AuthResponseDTO login(LoginDTO dto) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(qw);
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return AuthResponseDTO.builder()
                .accessToken(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .displayName(user.getNickname())
                .build();
    }

    @Override
    public String refreshToken(String refreshToken) {
        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
    }

    @Override
    public void logout(Long userId) {
        // JWT 无状态，暂不实现服务端黑名单
    }
}
