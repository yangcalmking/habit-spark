package com.habitspark.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.habitspark.dao.entity.TaskRecord;
import com.habitspark.dto.TaskReviewDTO;
import java.util.List;

public interface TaskService extends IService<TaskRecord> {
    IPage<TaskRecord> getPendingTasks(Long parentUserId, int page, int size);
    IPage<TaskRecord> getMyTasks(Long studentId, int page, int size);
    boolean submitTask(Long studentId, Long taskTemplateId, String description, String attachmentUrl);
    boolean reviewTask(TaskReviewDTO reviewDTO);
    boolean batchReview(List<TaskReviewDTO> reviews);
}
