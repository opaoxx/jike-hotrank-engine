package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.entity.Circle;
import com.jike.hotrank.engine.mapper.CircleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 圈子服务类
 * 提供圈子相关的业务操作
 *
 * @author JikeHotRank Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CircleService {

    private final CircleMapper circleMapper;

    /**
     * 根据ID查询圈子
     *
     * @param id 圈子ID
     * @return 圈子信息
     */
    public Circle getById(Long id) {
        return circleMapper.selectById(id);
    }

    /**
     * 查询所有启用的圈子
     *
     * @return 圈子列表
     */
    public List<Circle> listAllEnabled() {
        return circleMapper.selectAllEnabled();
    }

    /**
     * 创建圈子
     *
     * @param circle 圈子信息
     * @return 创建后的圈子（包含生成的ID）
     */
    public Circle create(Circle circle) {
        circle.setStatus(1);
        circleMapper.insert(circle);
        log.info("创建圈子成功：id={}, name={}", circle.getId(), circle.getName());
        return circle;
    }

    /**
     * 更新圈子
     *
     * @param circle 圈子信息
     * @return 更新行数
     */
    public int update(Circle circle) {
        int rows = circleMapper.updateById(circle);
        log.info("更新圈子成功：id={}, rows={}", circle.getId(), rows);
        return rows;
    }
}
