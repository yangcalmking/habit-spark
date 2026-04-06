package com.habitspark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.habitspark.dao.entity.Product;
import com.habitspark.dao.mapper.ProductMapper;
import com.habitspark.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;
    
    @Override
    public IPage<Product> getActiveProducts(int page, int size) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(Product::getIsActive, true).orderByAsc(Product::getSortOrder);
        return productMapper.selectPage(new Page<>(page, size), qw);
    }
    
    @Override
    public IPage<Product> getProductsByCategory(String category, int page, int size) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(Product::getCategory, category).eq(Product::getIsActive, true);
        return productMapper.selectPage(new Page<>(page, size), qw);
    }
    
    @Override
    public boolean createProduct(Product product) {
        return productMapper.insert(product) > 0;
    }
    
    @Override
    public boolean updateProduct(Product product) {
        product.setUpdatedAt(LocalDateTime.now());
        return productMapper.updateById(product) > 0;
    }
    
    @Override
    public boolean toggleProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product != null) {
            product.setIsActive(!product.getIsActive());
            product.setUpdatedAt(LocalDateTime.now());
            return productMapper.updateById(product) > 0;
        }
        return false;
    }
    
    @Override
    public List<Product> getLowStockProducts() {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(Product::getIsActive, true)
          .gt(Product::getStock, 0).le(Product::getStock, 5)
          .orderByAsc(Product::getStock);
        return productMapper.selectList(qw);
    }
}
