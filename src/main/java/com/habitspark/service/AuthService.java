package com.habitspark.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.habitspark.dao.entity.User;
import com.habitspark.dto.*;

public interface AuthService {
    AuthResponseDTO register(RegisterDTO registerDTO);
    AuthResponseDTO login(LoginDTO loginDTO);
    String refreshToken(String refreshToken);
    void logout(Long userId);
}
