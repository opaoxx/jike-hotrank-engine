package com.jike.hotrank.engine.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 圈子实体类
 * 对应数据库表：circle
 */
@Data
public class Circle {

    /** 圈子ID */
    private Long id;

    /** 圈子名称 */
    private String name;

    /** 圈子描述 */
    private String description;

    /** 圈子图标URL */
    private String iconUrl;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 排序权重 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
