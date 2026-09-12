package com.tokenhunter.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("usage_record")
public class UsageRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String provider;

    private String model;

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer totalTokens;

    private Integer durationMs;

    private Integer status;

    private Integer httpStatus;

    private String errorMessage;

    private String endpoint;

    private Integer stream;

    private LocalDateTime createdAt;
}
