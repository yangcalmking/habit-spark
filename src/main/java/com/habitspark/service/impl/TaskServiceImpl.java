package com.habitspark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.habitspark.dao.entity.*;
import com.habitspark.dao.mapper.*;
import com.habitspark.dto.TaskReviewDTO;
import com.habitspark.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRecordMapper taskRecordMapper;
    private final TaskTemplateMapper taskTemplateMapper;
    private final UserMapper userMapper;
    private final PointService pointService;

    @Override
    public IPage<TaskRecord> getPendingTasks(Long parentUserId, int page, int size) {
        // 查询父用户家庭组下所有学生的待审核任务
        User parent = userMapper.selectById(parentUserId);
        LambdaQueryWrapper<User> studentQw = new LambdaQueryWrapper<>();
        studentQw.eq(User::getFamilyGroupId, parent.getFamilyGroupId())
                 .eq(User::getRole, "student");
        List<User> students = userMapper.selectList(studentQw);
        List<Long> studentIds = students.stream().map(User::getId).toList();

        LambdaQueryWrapper<TaskRecord> qw = new LambdaQueryWrapper<>();
        qw.in(TaskRecord::getStudentId, studentIds)
          .eq(TaskRecord::getStatus, 0) // 0=pending
          .orderByAsc(TaskRecord::getCreatedAt);

        return taskRecordMapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public IPage<TaskRecord> getMyTasks(Long studentId, int page, int size) {
        LambdaQueryWrapper<TaskRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(TaskRecord::getStudentId, studentId)
          .orderByDesc(TaskRecord::getCreatedAt);
        return taskRecordMapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    @Transactional
    public boolean submitTask(Long studentId, Long taskTemplateId, String description, String attachmentUrl) {
        // 检查今日是否已提交
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<TaskRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(TaskRecord::getStudentId, studentId)
          .eq(TaskRecord::getTaskTemplateId, taskTemplateId)
          .eq(TaskRecord::getTaskDate, today);
        if (taskRecordMapper.selectCount(qw) > 0) {
            throw new RuntimeException("今日已提交该任务");
        }

        // 检查模板每日上限
        TaskTemplate template = taskTemplateMapper.selectById(taskTemplateId);
        if (template.getDailyCap() > 0) {
            LambdaQueryWrapper<TaskRecord> countQw = new LambdaQueryWrapper<>();
            countQw.eq(TaskRecord::getStudentId, studentId)
                   .eq(TaskRecord::getTaskTemplateId, taskTemplateId)
                   .eq(TaskRecord::getTaskDate, today)
                   .in(TaskRecord::getStatus, 0, 1);
            if (taskRecordMapper.selectCount(countQw) >= template.getDailyCap()) {
                throw new RuntimeException("今日该任务已达上限");
            }
        }

        TaskRecord record = new TaskRecord();
        record.setStudentId(studentId);
        record.setTaskTemplateId(taskTemplateId);
        record.setTaskDate(today);
        record.setDescription(description);
        record.setAttachmentUrl(attachmentUrl);
        record.setPoints(0); // 由后端根据模板规则计算
        record.setStatus(0); // pending

        return taskRecordMapper.insert(record) > 0;
    }

    @Override
    @Transactional
    public boolean reviewTask(TaskReviewDTO dto) {
        TaskRecord record = taskRecordMapper.selectById(dto.getTaskRecordId());
        if (record == null) {
            throw new RuntimeException("任务记录不存在");
        }
        if (record.getStatus() != 0) {
            throw new RuntimeException("任务已审核，不能重复操作");
        }

        // 根据审核结果计算积分
        if (dto.getApproved()) {
            TaskTemplate template = taskTemplateMapper.selectById(record.getTaskTemplateId());
            int points = template.getBasePoints() + template.getExtraPoints();
            record.setPoints(points);
            record.setStatus(1); // approved

            // 加分
            pointService.addPoints(record.getStudentId(), points, 
                "任务完成：" + template.getName(), dto.getReviewerId());
        } else {
            record.setStatus(2); // rejected
            record.setParentComment(dto.getComment());
        }

        record.setReviewerId(dto.getReviewerId());
        record.setReviewedAt(LocalDateTime.now());
        return taskRecordMapper.updateById(record) > 0;
    }

    @Override
    @Transactional
    public boolean batchReview(List<TaskReviewDTO> reviews) {
        for (TaskReviewDTO review : reviews) {
            reviewTask(review);
        }
        return true;
    }
}
