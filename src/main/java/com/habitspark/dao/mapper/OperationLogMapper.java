package com.habitspark.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.habitspark.dao.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
