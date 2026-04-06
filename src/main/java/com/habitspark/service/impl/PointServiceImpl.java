package com.habitspark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.habitspark.dao.entity.*;
import com.habitspark.dao.mapper.*;
import com.habitspark.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final PointAccountMapper pointAccountMapper;
    private final PointFlowMapper pointFlowMapper;
    private final UserMapper userMapper;

    @Override
    public PointAccount getAccountByStudentId(Long studentId) {
        LambdaQueryWrapper<PointAccount> qw = new LambdaQueryWrapper<>();
        qw.eq(PointAccount::getStudentId, studentId);
        return pointAccountMapper.selectOne(qw);
    }

    @Override
    public IPage<PointAccount> getAccounts(Long familyGroupId, int page, int size) {
        // 查询家庭组下所有学生的积分账户
        LambdaQueryWrapper<User> userQw = new LambdaQueryWrapper<>();
        userQw.eq(User::getFamilyGroupId, familyGroupId)
              .eq(User::getRole, "student");
        List<Long> studentIds = userMapper.selectList(userQw).stream()
                .map(User::getId).toList();

        LambdaQueryWrapper<PointAccount> qw = new LambdaQueryWrapper<>();
        qw.in(PointAccount::getStudentId, studentIds);
        return pointAccountMapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public List<PointFlow> getFlowHistory(Long studentId, int pageNum, int pageSize) {
        LambdaQueryWrapper<PointFlow> qw = new LambdaQueryWrapper<>();
        qw.eq(PointFlow::getStudentId, studentId)
          .orderByDesc(PointFlow::getCreatedAt);
        return pointFlowMapper.selectPage(new Page<>(pageNum, pageSize), qw).getRecords();
    }

    @Override
    @Transactional
    public boolean addPoints(Long studentId, int points, String reason, Long operatorId) {
        PointAccount account = getOrCreateAccount(studentId);
        account.setTotalPoints(account.getTotalPoints() + points);
        account.setAvailablePoints(account.getAvailablePoints() + points);
        pointAccountMapper.updateById(account);

        // 记录流水
        PointFlow flow = new PointFlow();
        flow.setStudentId(studentId);
        flow.setAmount(points);
        flow.setFlowType(1); // 任务获得
        flow.setReason(reason);
        flow.setOperatorId(operatorId);
        flow.setRelatedType("task");
        pointFlowMapper.insert(flow);

        return true;
    }

    @Override
    @Transactional
    public boolean deductPoints(Long studentId, int points, String reason, Long operatorId) {
        PointAccount account = getOrCreateAccount(studentId);
        if (account.getAvailablePoints() < points) {
            throw new RuntimeException("积分不足");
        }
        account.setTotalPoints(account.getTotalPoints() - points);
        account.setAvailablePoints(account.getAvailablePoints() - points);
        pointAccountMapper.updateById(account);

        PointFlow flow = new PointFlow();
        flow.setStudentId(studentId);
        flow.setAmount(-points);
        flow.setFlowType(2); // 家长扣分
        flow.setReason(reason);
        flow.setOperatorId(operatorId);
        flow.setRelatedType("adjust");
        pointFlowMapper.insert(flow);

        return true;
    }

    @Override
    @Transactional
    public boolean freezePoints(Long studentId, int points, String reason) {
        PointAccount account = getOrCreateAccount(studentId);
        if (account.getAvailablePoints() < points) {
            throw new RuntimeException("可用积分不足，无法冻结");
        }
        account.setAvailablePoints(account.getAvailablePoints() - points);
        account.setFrozenPoints(account.getFrozenPoints() + points);
        pointAccountMapper.updateById(account);

        PointFlow flow = new PointFlow();
        flow.setStudentId(studentId);
        flow.setAmount(-points);
        flow.setFlowType(5); // 兑换冻结
        flow.setReason(reason);
        pointFlowMapper.insert(flow);

        return true;
    }

    @Override
    @Transactional
    public boolean releaseFrozenPoints(Long studentId, int points) {
        PointAccount account = getOrCreateAccount(studentId);
        if (account.getFrozenPoints() < points) {
            throw new RuntimeException("冻结积分不足");
        }
        account.setFrozenPoints(account.getFrozenPoints() - points);
        account.setAvailablePoints(account.getAvailablePoints() + points);
        pointAccountMapper.updateById(account);

        PointFlow flow = new PointFlow();
        flow.setStudentId(studentId);
        flow.setAmount(points);
        flow.setFlowType(6); // 兑换解冻
        flow.setReason("兑换取消，冻结积分释放");
        pointFlowMapper.insert(flow);

        return true;
    }

    private PointAccount getOrCreateAccount(Long studentId) {
        PointAccount account = getAccountByStudentId(studentId);
        if (account == null) {
            account = new PointAccount();
            account.setStudentId(studentId);
            account.setTotalPoints(0);
            account.setAvailablePoints(0);
            account.setFrozenPoints(0);
            pointAccountMapper.insert(account);
        }
        return account;
    }
}
