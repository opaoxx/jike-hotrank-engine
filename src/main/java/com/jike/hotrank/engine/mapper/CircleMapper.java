package com.jike.hotrank.engine.mapper;

import com.jike.hotrank.engine.entity.Circle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 圈子Mapper接口
 */
@Mapper
public interface CircleMapper {

    /**
     * 根据ID查询圈子
     */
    Circle selectById(@Param("id") Long id);

    /**
     * 查询所有启用的圈子
     */
    List<Circle> selectAllEnabled();

    /**
     * 插入圈子
     */
    int insert(Circle circle);

    /**
     * 更新圈子
     */
    int updateById(Circle circle);
}
