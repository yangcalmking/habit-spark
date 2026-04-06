package com.habitspark.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.habitspark.dao.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
