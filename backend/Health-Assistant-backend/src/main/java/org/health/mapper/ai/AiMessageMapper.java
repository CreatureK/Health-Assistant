package org.health.mapper.ai;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.health.entity.ai.AiMessage;

import java.util.List;

/**
 * AI消息Mapper接口
 */
@Mapper
public interface AiMessageMapper {

    /**
     * 插入消息
     *
     * @param message 消息信息
     * @return 影响行数
     */
    int insert(AiMessage message);

    /**
     * 根据会话ID查询消息列表
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<AiMessage> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 根据会话ID查询最近N条消息
     *
     * @param sessionId 会话ID
     * @param limit 限制数量
     * @return 消息列表
     */
    List<AiMessage> selectRecentBySessionId(@Param("sessionId") Long sessionId, @Param("limit") Integer limit);
}

