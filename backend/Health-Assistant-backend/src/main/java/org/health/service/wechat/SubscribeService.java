package org.health.service.wechat;

import org.health.common.UserContext;
import org.health.entity.wechat.SubscribeGrant;
import org.health.mapper.wechat.SubscribeGrantMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 订阅消息服务
 */
@Service
public class SubscribeService {

    @Autowired
    private SubscribeGrantMapper subscribeGrantMapper;

    @Value("${wechat.subscribe.template-ids:TEMPLATE_ID_1}")
    private String templateIdsConfig;

    /**
     * 获取订阅模板配置
     *
     * @return 模板ID列表
     */
    public SubscribeConfigVO getSubscribeConfig() {
        SubscribeConfigVO vo = new SubscribeConfigVO();
        // 从配置中读取模板ID列表（这里简化处理，实际应该从配置文件或数据库读取）
        String[] templateIds = templateIdsConfig.split(",");
        vo.setTemplateIds(List.of(templateIds));
        return vo;
    }

    /**
     * 上报授权结果
     *
     * @param request 授权结果请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void reportGrant(ReportGrantRequest request) {
        Long userId = UserContext.getUserId();

        // 查询现有授权信息
        SubscribeGrant grant = subscribeGrantMapper.selectByUserId(userId);

        if (grant == null) {
            // 创建新授权记录
            grant = new SubscribeGrant();
            grant.setUserId(userId);
            grant.setGranted(request.getGranted());
            grant.setTemplateIds(request.getDetail() != null ?
                    request.getDetail().keySet().stream().toList() : null);
            grant.setDetail(request.getDetail());
            subscribeGrantMapper.insert(grant);
        } else {
            // 更新授权信息
            grant.setGranted(request.getGranted());
            if (request.getDetail() != null) {
                grant.setTemplateIds(request.getDetail().keySet().stream().toList());
                grant.setDetail(request.getDetail());
            }
            subscribeGrantMapper.update(grant);
        }
    }

    /**
     * 订阅配置视图对象
     */
    public static class SubscribeConfigVO {
        private List<String> templateIds;

        public List<String> getTemplateIds() { return templateIds; }
        public void setTemplateIds(List<String> templateIds) { this.templateIds = templateIds; }
    }

    /**
     * 上报授权结果请求
     */
    public static class ReportGrantRequest {
        private Boolean granted;
        private Map<String, String> detail;

        public Boolean getGranted() { return granted; }
        public void setGranted(Boolean granted) { this.granted = granted; }
        public Map<String, String> getDetail() { return detail; }
        public void setDetail(Map<String, String> detail) { this.detail = detail; }
    }
}

