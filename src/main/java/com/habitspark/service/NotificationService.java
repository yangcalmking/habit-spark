package com.habitspark.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.habitspark.dao.entity.Notification;
import java.util.List;

public interface NotificationService extends IService<Notification> {
    List<Notification> getUnread(Long userId);
    boolean markAsRead(Long notificationId);
    boolean markAllAsRead(Long userId);
    boolean sendNotification(Long userId, String title, String content, String type);
    IPage<Notification> getUserNotifications(Long userId, int page, int size);
}
