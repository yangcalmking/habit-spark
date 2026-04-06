package com.habitspark.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.habitspark.dao.entity.Product;

public interface ProductService extends IService<Product> {
    IPage<Product> getActiveProducts(int page, int size);
    IPage<Product> getProductsByCategory(String category, int page, int size);
    Product createProduct(Product product, Long operatorId);
    boolean updateProduct(Product product, Long operatorId);
    boolean toggleProduct(Long productId, Long operatorId);
}
