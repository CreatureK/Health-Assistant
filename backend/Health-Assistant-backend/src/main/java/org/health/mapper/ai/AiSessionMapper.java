package org.health.mapper.ai;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.health.entity.ai.AiSession;

import java.util.List;

/**
 * AI会话Mapper接口
 */
@Mapper
public interface AiSessionMapper {

    /**
     * 根据ID查询会话
     *
     * @param id 会话ID
     * @return 会话信息
     */
    AiSession selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<AiSession> selectByUserId(@Param("userId") Long userId);

    /**
     * 插入会话
     *
     * @param session 会话信息
     * @return 影响行数
     */
    int insert(AiSession session);

    /**
     * 更新会话
     *
     * @param session 会话信息
     * @return 影响行数
     */
    int update(AiSession session);

    /**
     * 软删除会话
     *
     * @param id 会话ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}

