package org.health.mapper.wechat;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.health.entity.wechat.SubscribeGrant;

/**
 * 订阅授权Mapper接口
 */
@Mapper
public interface SubscribeGrantMapper {

    /**
     * 根据用户ID查询授权信息
     *
     * @param userId 用户ID
     * @return 授权信息
     */
    SubscribeGrant selectByUserId(@Param("userId") Long userId);

    /**
     * 插入授权信息
     *
     * @param grant 授权信息
     * @return 影响行数
     */
    int insert(SubscribeGrant grant);

    /**
     * 更新授权信息
     *
     * @param grant 授权信息
     * @return 影响行数
     */
    int update(SubscribeGrant grant);
}

