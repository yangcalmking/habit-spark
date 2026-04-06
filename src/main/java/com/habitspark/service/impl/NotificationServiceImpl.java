package com.habitspark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.habitspark.dao.entity.Notification;
import com.habitspark.dao.mapper.NotificationMapper;
import com.habitspark.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public List<Notification> getUnread(Long userId) {
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<>();
        qw.eq(Notification::getUserId, userId)
          .eq(Notification::getIsRead, false)
          .orderByDesc(Notification::getCreatedAt);
        return notificationMapper.selectList(qw);
    }

    @Override
    public boolean markAsRead(Long notificationId) {
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setIsRead(true);
        return notificationMapper.updateById(notification) > 0;
    }

    @Override
    public boolean markAllAsRead(Long userId) {
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<>();
        qw.eq(Notification::getUserId, userId)
          .eq(Notification::getIsRead, false);
        
        Notification update = new Notification();
        update.setIsRead(true);
        return notificationMapper.update(update, qw) > 0;
    }

    @Override
    public boolean sendNotification(Long userId, String title, String content, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(false);
        return notificationMapper.insert(notification) > 0;
    }

    @Override
    public IPage<Notification> getUserNotifications(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<>();
        qw.eq(Notification::getUserId, userId)
          .orderByDesc(Notification::getCreatedAt);
        return notificationMapper.selectPage(new Page<>(page, size), qw);
    }
}
