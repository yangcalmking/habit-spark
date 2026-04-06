package com.habitspark.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.habitspark.dao.entity.ExchangeRequest;
import com.habitspark.dto.ExchangeDTO;
import java.util.List;

public interface ExchangeService extends IService<ExchangeRequest> {
    ExchangeRequest submitExchange(Long studentId, Long productId);
    IPage<ExchangeRequest> getMyExchanges(Long studentId, int page, int size);
    boolean approveExchange(Long requestId, Long reviewerId);
    boolean rejectExchange(Long requestId, Long reviewerId);
    boolean autoApproveInstant(Long requestId);
}
