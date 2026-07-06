package com.jike.hotrank.engine.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskLockMapper {

    @Select("SELECT GET_LOCK(#{lockName}, #{timeoutSeconds})")
    Integer getLock(@Param("lockName") String lockName, @Param("timeoutSeconds") int timeoutSeconds);

    @Select("SELECT RELEASE_LOCK(#{lockName})")
    Integer releaseLock(@Param("lockName") String lockName);
}
