package com.habitspark.dao.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("exchange_request")
public class ExchangeRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long productId;
    private Integer pointsCost;
    private String status;
    private Long reviewerId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private LocalDateTime confirmTime;
}
