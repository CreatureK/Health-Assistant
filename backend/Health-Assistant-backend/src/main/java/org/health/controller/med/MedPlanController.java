package org.health.controller.med;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.health.common.Result;
import org.health.common.ResultCode;
import org.health.service.med.MedPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 用药计划控制器
 */
@Tag(name = "用药计划管理", description = "用药计划的创建、查询、更新、删除等接口")
@RestController
@RequestMapping("/med/plans")
public class MedPlanController {

    @Autowired
    private MedPlanService medPlanService;

    /**
     * 获取计划列表
     * GET /api/v1/med/plans
     */
    @Operation(summary = "获取计划列表", description = "支持按状态和关键词过滤")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = MedPlanService.MedPlanVO.class)))
    })
    @GetMapping
    public Result<List<MedPlanService.MedPlanVO>> getPlanList(
            @Parameter(description = "状态过滤：active-进行中，expired-已过期")
            @RequestParam(required = false) String status,
            @Parameter(description = "关键词搜索")
            @RequestParam(required = false) String keyword) {
        List<MedPlanService.MedPlanVO> plans = medPlanService.getPlanList(status, keyword);
        return Result.success(plans);
    }

    /**
     * 创建计划
     * POST /api/v1/med/plans
     */
    @Operation(summary = "创建用药计划", description = "创建新的用药计划")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping
    public Result<CreatePlanResponse> createPlan(
            @Parameter(description = "创建计划请求参数", required = true)
            @Valid @RequestBody CreatePlanRequest request) {
        try {
            MedPlanService.CreatePlanRequest serviceRequest = new MedPlanService.CreatePlanRequest();
            serviceRequest.setName(request.getName());
            serviceRequest.setDosage(request.getDosage());
            serviceRequest.setTimes(request.getTimes());
            serviceRequest.setStartDate(request.getStartDate());
            serviceRequest.setEndDate(request.getEndDate());
            serviceRequest.setRepeatType(request.getRepeatType());
            serviceRequest.setRepeatDays(request.getRepeatDays());
            serviceRequest.setRemindEnabled(request.getRemindEnabled());

            Long planId = medPlanService.createPlan(serviceRequest);
            CreatePlanResponse response = new CreatePlanResponse();
            response.setId(planId);
            return Result.success(response);
        } catch (RuntimeException e) {
            return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 获取计划详情
     * GET /api/v1/med/plans/:id
     */
    @Operation(summary = "获取计划详情", description = "根据计划ID获取详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "计划不存在")
    })
    @GetMapping("/{id}")
    public Result<MedPlanService.MedPlanVO> getPlanDetail(
            @Parameter(description = "计划ID", required = true)
            @PathVariable Long id) {
        try {
            MedPlanService.MedPlanVO plan = medPlanService.getPlanDetail(id);
            return Result.success(plan);
        } catch (RuntimeException e) {
            return Result.error(ResultCode.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 更新计划
     * PUT /api/v1/med/plans/:id
     */
    @Operation(summary = "更新用药计划", description = "更新计划信息，会重建today+1之后的点位记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "404", description = "计划不存在")
    })
    @PutMapping("/{id}")
    public Result<Void> updatePlan(
            @Parameter(description = "计划ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "更新计划请求参数", required = true)
            @Valid @RequestBody UpdatePlanRequest request) {
        try {
            MedPlanService.UpdatePlanRequest serviceRequest = new MedPlanService.UpdatePlanRequest();
            serviceRequest.setName(request.getName());
            serviceRequest.setDosage(request.getDosage());
            serviceRequest.setTimes(request.getTimes());
            serviceRequest.setStartDate(request.getStartDate());
            serviceRequest.setEndDate(request.getEndDate());
            serviceRequest.setRepeatType(request.getRepeatType());
            serviceRequest.setRepeatDays(request.getRepeatDays());
            serviceRequest.setRemindEnabled(request.getRemindEnabled());

            medPlanService.updatePlan(id, serviceRequest);
            return Result.success();
        } catch (RuntimeException e) {
            if (e.getMessage().equals(ResultCode.NOT_FOUND.getMsg())) {
                return Result.error(ResultCode.NOT_FOUND, e.getMessage());
            }
            return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 删除计划
     * DELETE /api/v1/med/plans/:id
     */
    @Operation(summary = "删除用药计划", description = "软删除计划，级联删除未来todo records")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "计划不存在")
    })
    @DeleteMapping("/{id}")
    public Result<Void> deletePlan(
            @Parameter(description = "计划ID", required = true)
            @PathVariable Long id) {
        try {
            medPlanService.deletePlan(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(ResultCode.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 更新提醒开关
     * POST /api/v1/med/plans/:id/remind
     */
    @Operation(summary = "更新提醒开关", description = "开启或关闭计划的微信提醒")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "计划不存在")
    })
    @PostMapping("/{id}/remind")
    public Result<RemindResponse> updateRemind(
            @Parameter(description = "计划ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "提醒开关请求参数", required = true)
            @Valid @RequestBody RemindRequest request) {
        try {
            medPlanService.updateRemindEnabled(id, request.getRemindEnabled());
            RemindResponse response = new RemindResponse();
            response.setRemindEnabled(request.getRemindEnabled());
            return Result.success(response);
        } catch (RuntimeException e) {
            return Result.error(ResultCode.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 创建计划请求
     */
    @Schema(description = "创建计划请求参数")
    public static class CreatePlanRequest {
        @Schema(description = "药品名称", example = "降压药", required = true)
        @NotBlank(message = "药品名称不能为空")
        private String name;

        @Schema(description = "剂量", example = "1片", required = true)
        @NotBlank(message = "剂量不能为空")
        private String dosage;

        @Schema(description = "服用时间列表", example = "[\"08:00\", \"20:00\"]", required = true)
        @NotNull(message = "服用时间不能为空")
        private List<String> times;

        @Schema(description = "开始日期", example = "2025-12-18", required = true)
        @NotNull(message = "开始日期不能为空")
        private LocalDate startDate;

        @Schema(description = "结束日期", example = "2026-01-18", required = true)
        @NotNull(message = "结束日期不能为空")
        private LocalDate endDate;

        @Schema(description = "重复类型", example = "daily", allowableValues = {"daily", "weekly"}, required = true)
        @NotBlank(message = "重复类型不能为空")
        private String repeatType;

        @Schema(description = "重复天数（weekly类型需要，0=周日，6=周六）", example = "[1,2,3,4,5]")
        private List<Integer> repeatDays;

        @Schema(description = "提醒开关", example = "true")
        private Boolean remindEnabled;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }
        public List<String> getTimes() { return times; }
        public void setTimes(List<String> times) { this.times = times; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getRepeatType() { return repeatType; }
        public void setRepeatType(String repeatType) { this.repeatType = repeatType; }
        public List<Integer> getRepeatDays() { return repeatDays; }
        public void setRepeatDays(List<Integer> repeatDays) { this.repeatDays = repeatDays; }
        public Boolean getRemindEnabled() { return remindEnabled; }
        public void setRemindEnabled(Boolean remindEnabled) { this.remindEnabled = remindEnabled; }
    }

    /**
     * 更新计划请求
     */
    @Schema(description = "更新计划请求参数")
    public static class UpdatePlanRequest {
        @Schema(description = "药品名称", example = "降压药", required = true)
        @NotBlank(message = "药品名称不能为空")
        private String name;

        @Schema(description = "剂量", example = "1片", required = true)
        @NotBlank(message = "剂量不能为空")
        private String dosage;

        @Schema(description = "服用时间列表", example = "[\"08:00\", \"20:00\"]", required = true)
        @NotNull(message = "服用时间不能为空")
        private List<String> times;

        @Schema(description = "开始日期", example = "2025-12-18", required = true)
        @NotNull(message = "开始日期不能为空")
        private LocalDate startDate;

        @Schema(description = "结束日期", example = "2026-01-18", required = true)
        @NotNull(message = "结束日期不能为空")
        private LocalDate endDate;

        @Schema(description = "重复类型", example = "daily", allowableValues = {"daily", "weekly"}, required = true)
        @NotBlank(message = "重复类型不能为空")
        private String repeatType;

        @Schema(description = "重复天数（weekly类型需要，0=周日，6=周六）", example = "[1,2,3,4,5]")
        private List<Integer> repeatDays;

        @Schema(description = "提醒开关", example = "true")
        private Boolean remindEnabled;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }
        public List<String> getTimes() { return times; }
        public void setTimes(List<String> times) { this.times = times; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getRepeatType() { return repeatType; }
        public void setRepeatType(String repeatType) { this.repeatType = repeatType; }
        public List<Integer> getRepeatDays() { return repeatDays; }
        public void setRepeatDays(List<Integer> repeatDays) { this.repeatDays = repeatDays; }
        public Boolean getRemindEnabled() { return remindEnabled; }
        public void setRemindEnabled(Boolean remindEnabled) { this.remindEnabled = remindEnabled; }
    }

    /**
     * 创建计划响应
     */
    @Schema(description = "创建计划响应")
    public static class CreatePlanResponse {
        @Schema(description = "计划ID", example = "123")
        private Long id;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    /**
     * 提醒开关请求
     */
    @Schema(description = "提醒开关请求参数")
    public static class RemindRequest {
        @Schema(description = "提醒开关", example = "true", required = true)
        @NotNull(message = "提醒开关不能为空")
        private Boolean remindEnabled;

        public Boolean getRemindEnabled() { return remindEnabled; }
        public void setRemindEnabled(Boolean remindEnabled) { this.remindEnabled = remindEnabled; }
    }

    /**
     * 提醒开关响应
     */
    @Schema(description = "提醒开关响应")
    public static class RemindResponse {
        @Schema(description = "提醒开关", example = "true")
        private Boolean remindEnabled;

        public Boolean getRemindEnabled() { return remindEnabled; }
        public void setRemindEnabled(Boolean remindEnabled) { this.remindEnabled = remindEnabled; }
    }
}

