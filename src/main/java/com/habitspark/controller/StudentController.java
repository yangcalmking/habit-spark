package com.habitspark.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.habitspark.dao.entity.TaskRecord;
import com.habitspark.dao.entity.PointAccount;
import com.habitspark.dao.entity.Notification;
import com.habitspark.dto.TaskSubmitDTO;
import com.habitspark.service.TaskService;
import com.habitspark.service.PointService;
import com.habitspark.service.ExchangeService;
import com.habitspark.service.NotificationService;
import com.habitspark.service.ProductService;
import com.habitspark.util.JwtUtil;
import com.habitspark.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final TaskService taskService;
    private final PointService pointService;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;
    private final ProductService productService;

    // ========== 任务相关 ==========

    @GetMapping("/tasks")
    public Result<IPage<TaskRecord>> getMyTasks(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long studentId = JwtUtil.getUserId(token);
        return Result.ok(taskService.getMyTasks(studentId, page, size));
    }

    @PostMapping("/tasks")
    public Result<Void> submitTask(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody TaskSubmitDTO dto) {
        Long studentId = JwtUtil.getUserId(token);
        taskService.submitTask(studentId, dto.getTaskTemplateId(),
                dto.getDescription(), dto.getAttachmentUrl());
        return Result.ok();
    }

    @GetMapping("/tasks/pending")
    public Result<IPage<TaskRecord>> getPendingTasks(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long studentId = JwtUtil.getUserId(token);
        // 学生查看自己的待审核任务
        return Result.ok(null); // 可复用 getMyTasks 过滤 status=0
    }

    // ========== 积分相关 ==========

    @GetMapping("/points")
    public Result<PointAccount> getMyPoints(
            @RequestHeader("Authorization") String token) {
        Long studentId = JwtUtil.getUserId(token);
        return Result.ok(pointService.getAccountByStudentId(studentId));
    }

    @GetMapping("/points/history")
    public Result<?> getPointHistory(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long studentId = JwtUtil.getUserId(token);
        return Result.ok(pointService.getFlowHistory(studentId, page, size));
    }

    // ========== 通知相关 ==========

    @GetMapping("/notifications/unread")
    public Result<List<Notification>> getUnreadNotifications(
            @RequestHeader("Authorization") String token) {
        Long userId = JwtUtil.getUserId(token);
        return Result.ok(notificationService.getUnread(userId));
    }

    @GetMapping("/notifications")
    public Result<?> getNotifications(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = JwtUtil.getUserId(token);
        return Result.ok(notificationService.getUserNotifications(userId, page, size));
    }

    @PostMapping("/notifications/{id}/read")
    public Result<Void> markAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.ok();
    }

    @PostMapping("/notifications/read-all")
    public Result<Void> markAllAsRead(
            @RequestHeader("Authorization") String token) {
        Long userId = JwtUtil.getUserId(token);
        notificationService.markAllAsRead(userId);
        return Result.ok();
    }
}
