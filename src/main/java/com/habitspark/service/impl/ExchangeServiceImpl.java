package com.habitspark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.habitspark.dao.entity.*;
import com.habitspark.dao.mapper.*;
import com.habitspark.dto.ExchangeDTO;
import com.habitspark.service.ExchangeService;
import com.habitspark.service.NotificationService;
import com.habitspark.service.PointService;
import com.habitspark.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExchangeServiceImpl implements ExchangeService {

    private final ExchangeRequestMapper exchangeRequestMapper;
    private final ProductMapper productMapper;
    private final PointService pointService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ExchangeRequest submitExchange(Long studentId, ExchangeDTO dto) {
        // 1. 查询商品信息
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || !product.getIsActive()) {
            throw new RuntimeException("商品不存在或已下架");
        }
        if (product.getStock() == 0) {
            throw new RuntimeException("商品已兑完");
        }

        // 2. 冻结积分
        pointService.freezePoints(studentId, product.getPointsCost(),
                "兑换商品：" + product.getName());

        // 3. 创建兑换申请
        ExchangeRequest request = new ExchangeRequest();
        request.setStudentId(studentId);
        request.setProductId(product.getId());
        request.setPointsCost(product.getPointsCost());
        request.setStatus("pending");
        request.setProductSnapshot(product.getName());
        request.setAutoConfirm("instant".equals(product.getLevel()));

        exchangeRequestMapper.insert(request);

        // 4. 扣减库存
        if (product.getStock() > 0) {
            product.setStock(product.getStock() - 1);
            productMapper.updateById(product);
        }

        // 5. 如果是即时奖励 (≤15分)，自动确认
        if ("instant".equals(product.getLevel())) {
            autoConfirm(request.getId(), product);
        }

        return request;
    }

    @Override
    public IPage<ExchangeRequest> getMyExchanges(Long studentId, int page, int size) {
        LambdaQueryWrapper<ExchangeRequest> qw = new LambdaQueryWrapper<>();
        qw.eq(ExchangeRequest::getStudentId, studentId)
                .orderByDesc(ExchangeRequest::getCreatedAt);
        return exchangeRequestMapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    @Transactional
    public boolean confirmExchange(Long requestId, Long reviewerId) {
        ExchangeRequest request = exchangeRequestMapper.selectById(requestId);
        if (request == null) throw new RuntimeException("兑换记录不存在");
        if (!"pending".equals(request.getStatus())) throw new RuntimeException("状态错误");

        Product product = productMapper.selectById(request.getProductId());

        // 扣除冻结积分
        pointService.deductFrozenPoints(request.getStudentId(), request.getPointsCost());

        // 更新状态
        request.setStatus("delivered");
        request.setReviewerId(reviewerId);
        request.setReviewedAt(LocalDateTime.now());
        exchangeRequestMapper.updateById(request);

        // 通知学生
        notificationService.sendNotification(request.getStudentId(),
                "兑换已确认",
                "您的兑换 [" + request.getProductSnapshot() + "] 已确认，积分已扣除。",
                "exchange_confirmed");

        return true;
    }

    @Override
    @Transactional
    public boolean rejectExchange(Long requestId, Long reviewerId) {
        ExchangeRequest request = exchangeRequestMapper.selectById(requestId);
        if (request == null) throw new RuntimeException("兑换记录不存在");
        if (!"pending".equals(request.getStatus())) throw new RuntimeException("状态错误");

        // 释放冻结积分
        pointService.releaseFrozenPoints(request.getStudentId(), request.getPointsCost());

        request.setStatus("canceled");
        request.setReviewerId(reviewerId);
        request.setReviewedAt(LocalDateTime.now());
        exchangeRequestMapper.updateById(request);

        // 恢复库存
        Product product = productMapper.selectById(request.getProductId());
        if (product != null && product.getStock() >= 0) {
            product.setStock(product.getStock() + 1);
            productMapper.updateById(product);
        }

        notificationService.sendNotification(request.getStudentId(),
                "兑换已取消",
                "您的兑换 [" + request.getProductSnapshot() + "] 已被取消，积分已返还。",
                "exchange_canceled");

        return true;
    }

    private void autoConfirm(Long requestId, Product product) {
        ExchangeRequest request = exchangeRequestMapper.selectById(requestId);
        request.setStatus("delivered");
        request.setReviewedAt(LocalDateTime.now());
        exchangeRequestMapper.updateById(request);

        // 扣除冻结积分
        pointService.deductFrozenPoints(request.getStudentId(), request.getPointsCost());

        notificationService.sendNotification(request.getStudentId(),
                "兑换成功",
                "您的兑换 [" + product.getName() + "] 已自动确认！",
                "exchange_confirmed");
    }
}
