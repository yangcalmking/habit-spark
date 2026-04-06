package com.habitspark.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.habitspark.dao.entity.*;
import com.habitspark.dto.*;
import com.habitspark.service.*;
import com.habitspark.util.JwtUtil;
import com.habitspark.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/parent")
@RequiredArgsConstructor
public class ParentController {

    private final TaskService taskService;
    private final PointService pointService;
    private final ProductService productService;
    private final ExchangeService exchangeService;
    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    // ========== 任务审核 ==========

    @GetMapping("/tasks/pending")
    public Result<IPage<TaskRecord>> getPendingTasks(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long parentUserId = JwtUtil.getUserId(token);
        return Result.ok(taskService.getPendingTasks(parentUserId, page, size));
    }

    @PostMapping("/tasks/review")
    public Result<Void> reviewTask(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody TaskReviewDTO dto) {
        Long reviewerId = JwtUtil.getUserId(token);
        dto.setReviewerId(reviewerId);
        taskService.reviewTask(dto);
        return Result.ok();
    }

    @PostMapping("/tasks/batch-review")
    public Result<Void> batchReview(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody List<TaskReviewDTO> reviews) {
        Long reviewerId = JwtUtil.getUserId(token);
        reviews.forEach(r -> r.setReviewerId(reviewerId));
        taskService.batchReview(reviews);
        return Result.ok();
    }

    @GetMapping("/tasks/history")
    public Result<IPage<TaskRecord>> getTaskHistory(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long parentUserId = JwtUtil.getUserId(token);
        // TODO: 查询家庭组下所有学生的历史记录
        return Result.ok(taskService.getPendingTasks(parentUserId, page, size));
    }

    // ========== 积分管理 ==========

    @GetMapping("/students/points")
    public Result<List<PointAccount>> getAllStudentPoints(
            @RequestHeader("Authorization") String token) {
        Long parentUserId = JwtUtil.getUserId(token);
        // TODO: 查询家庭组下所有学生的积分
        return Result.ok(null);
    }

    @PostMapping("/points/adjust")
    public Result<Void> adjustPoints(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody PointAdjustDTO dto) {
        Long operatorId = JwtUtil.getUserId(token);
        if (dto.getPoints() > 0) {
            pointService.addPoints(dto.getStudentId(), dto.getPoints(), dto.getReason(), operatorId);
        } else {
            pointService.deductPoints(dto.getStudentId(), Math.abs(dto.getPoints()), dto.getReason(), operatorId);
        }
        return Result.ok();
    }

    @GetMapping("/points/history")
    public Result<?> getPointHistory(
            @RequestHeader("Authorization") String token,
            @RequestParam Long studentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(pointService.getFlowHistory(studentId, page, size));
    }

    // ========== 商品管理 ==========

    @GetMapping("/products")
    public Result<IPage<Product>> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(productService.getActiveProducts(page, size));
    }

    @GetMapping("/products/category/{category}")
    public Result<IPage<Product>> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(productService.getProductsByCategory(category, page, size));
    }

    @PostMapping("/products")
    public Result<Product> createProduct(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody Product product) {
        boolean created = productService.createProduct(product);
        return created ? Result.ok(product) : Result.error("创建失败");
    }

    @PutMapping("/products/{id}")
    public Result<Void> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody Product product) {
        product.setId(id);
        productService.updateProduct(product);
        return Result.ok();
    }

    @PostMapping("/products/{id}/toggle")
    public Result<Void> toggleProduct(@PathVariable Long id) {
        productService.toggleProduct(id);
        return Result.ok();
    }

    // ========== 兑换审核 ==========

    @PostMapping("/exchanges/{id}/confirm")
    public Result<Void> confirmExchange(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        Long reviewerId = JwtUtil.getUserId(token);
        exchangeService.confirmExchange(id, reviewerId);
        return Result.ok();
    }

    @PostMapping("/exchanges/{id}/reject")
    public Result<Void> rejectExchange(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        Long reviewerId = JwtUtil.getUserId(token);
        exchangeService.rejectExchange(id, reviewerId);
        return Result.ok();
    }

    // ========== 通知 ==========

    @GetMapping("/notifications")
    public Result<?> getNotifications(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = JwtUtil.getUserId(token);
        return Result.ok(notificationService.getUserNotifications(userId, page, size));
    }
}
