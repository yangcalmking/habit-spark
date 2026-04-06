package com.habitspark.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.habitspark.dao.entity.PointAccount;
import com.habitspark.dao.entity.PointFlow;
import java.util.List;

public interface PointService extends IService<PointAccount> {
    PointAccount getAccountByStudentId(Long studentId);
    IPage<PointAccount> getAccounts(Long familyGroupId, int page, int size);
    List<PointFlow> getFlowHistory(Long studentId, int pageNum, int pageSize);
    boolean addPoints(Long studentId, int points, String reason, Long operatorId);
    boolean deductPoints(Long studentId, int points, String reason, Long operatorId);
    boolean freezePoints(Long studentId, int points, String reason);
    boolean releaseFrozenPoints(Long studentId, int points);
}
