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
import org.health.service.med.MedRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用药记录控制器
 */
@Tag(name = "用药记录管理", description = "用药记录的查询、标记、补记等接口")
@RestController
@RequestMapping("/med/records")
public class MedRecordController {

    @Autowired
    private MedRecordService medRecordService;

    /**
     * 查询记录
     * GET /api/v1/med/records
     */
    @Operation(summary = "查询记录", description = "支持按计划、状态、日期区间过滤")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = MedRecordService.RecordListVO.class)))
    })
    @GetMapping
    public Result<MedRecordService.RecordListVO> getRecordList(
            @Parameter(description = "计划ID")
            @RequestParam(required = false) Long planId,
            @Parameter(description = "状态：todo|taken|missed")
            @RequestParam(required = false) String status,
            @Parameter(description = "开始日期")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "页码", example = "1")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(required = false) Integer size) {
        MedRecordService.RecordListVO result = medRecordService.getRecordList(planId, status, startDate, endDate, page, size);
        return Result.success(result);
    }

    /**
     * 标记已服用/未服
     * POST /api/v1/med/records/:recordId/mark
     */
    @Operation(summary = "标记已服用/未服", description = "标记记录为已服用或未服，actionAt默认写入当前时间")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "标记成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "404", description = "记录不存在")
    })
    @PostMapping("/{recordId}/mark")
    public Result<Void> markRecord(
            @Parameter(description = "记录ID", required = true)
            @PathVariable Long recordId,
            @Parameter(description = "标记请求参数", required = true)
            @Valid @RequestBody MarkRecordRequest request) {
        try {
            medRecordService.markRecord(recordId, request.getStatus());
            return Result.success();
        } catch (RuntimeException e) {
            if (e.getMessage().equals(ResultCode.NOT_FOUND.getMsg())) {
                return Result.error(ResultCode.NOT_FOUND, e.getMessage());
            }
            return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 补记/更正
     * POST /api/v1/med/records/:recordId/adjust
     */
    @Operation(summary = "补记/更正", description = "补记或更正记录，可填写实际操作时间和备注")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "404", description = "记录不存在")
    })
    @PostMapping("/{recordId}/adjust")
    public Result<Void> adjustRecord(
            @Parameter(description = "记录ID", required = true)
            @PathVariable Long recordId,
            @Parameter(description = "补记请求参数", required = true)
            @Valid @RequestBody AdjustRecordRequest request) {
        try {
            MedRecordService.AdjustRecordRequest serviceRequest = new MedRecordService.AdjustRecordRequest();
            serviceRequest.setStatus(request.getStatus());
            serviceRequest.setActionAt(request.getActionAt());
            serviceRequest.setNote(request.getNote());
            medRecordService.adjustRecord(recordId, serviceRequest);
            return Result.success();
        } catch (RuntimeException e) {
            if (e.getMessage().equals(ResultCode.NOT_FOUND.getMsg())) {
                return Result.error(ResultCode.NOT_FOUND, e.getMessage());
            }
            return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 标记记录请求
     */
    @Schema(description = "标记记录请求参数")
    public static class MarkRecordRequest {
        @Schema(description = "状态", example = "taken", allowableValues = {"taken", "missed"}, required = true)
        @NotBlank(message = "状态不能为空")
        private String status;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /**
     * 补记请求
     */
    @Schema(description = "补记请求参数")
    public static class AdjustRecordRequest {
        @Schema(description = "状态", example = "taken", allowableValues = {"taken", "missed"}, required = true)
        @NotBlank(message = "状态不能为空")
        private String status;

        @Schema(description = "实际操作时间", example = "2025-12-18 08:10:00")
        private LocalDateTime actionAt;

        @Schema(description = "备注", example = "忘记打卡，补记")
        private String note;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getActionAt() { return actionAt; }
        public void setActionAt(LocalDateTime actionAt) { this.actionAt = actionAt; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}

