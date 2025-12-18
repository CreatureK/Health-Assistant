package org.health.controller.wechat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.health.common.Result;
import org.health.service.wechat.SubscribeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 微信订阅消息控制器
 */
@Tag(name = "微信订阅消息管理", description = "订阅模板配置和授权结果上报接口")
@RestController
@RequestMapping("/wechat/subscribe")
public class SubscribeController {

    @Autowired
    private SubscribeService subscribeService;

    /**
     * 获取订阅模板配置
     * GET /api/v1/wechat/subscribe/config
     */
    @Operation(summary = "获取订阅模板配置", description = "获取可用的订阅消息模板ID列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = SubscribeService.SubscribeConfigVO.class)))
    })
    @GetMapping("/config")
    public Result<SubscribeService.SubscribeConfigVO> getSubscribeConfig() {
        SubscribeService.SubscribeConfigVO result = subscribeService.getSubscribeConfig();
        return Result.success(result);
    }

    /**
     * 上报授权结果
     * POST /api/v1/wechat/subscribe/report
     */
    @Operation(summary = "上报授权结果", description = "上报用户对订阅消息的授权结果")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上报成功")
    })
    @PostMapping("/report")
    public Result<Void> reportGrant(
            @Parameter(description = "授权结果请求参数", required = true)
            @Valid @RequestBody ReportGrantRequest request) {
        SubscribeService.ReportGrantRequest serviceRequest = new SubscribeService.ReportGrantRequest();
        serviceRequest.setGranted(request.getGranted());
        serviceRequest.setDetail(request.getDetail());
        subscribeService.reportGrant(serviceRequest);
        return Result.success();
    }

    /**
     * 上报授权结果请求
     */
    @Schema(description = "上报授权结果请求参数")
    public static class ReportGrantRequest {
        @Schema(description = "是否授权", example = "true", required = true)
        @NotNull(message = "授权状态不能为空")
        private Boolean granted;

        @Schema(description = "授权详情", example = "{\"TEMPLATE_ID_1\": \"accept\"}")
        private Map<String, String> detail;

        public Boolean getGranted() { return granted; }
        public void setGranted(Boolean granted) { this.granted = granted; }
        public Map<String, String> getDetail() { return detail; }
        public void setDetail(Map<String, String> detail) { this.detail = detail; }
    }
}

