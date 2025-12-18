package org.health.controller.med;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.health.common.Result;
import org.health.service.med.MedTodayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 今日用药控制器
 */
@Tag(name = "今日用药管理", description = "今日用药打卡相关接口")
@RestController
@RequestMapping("/med/today")
public class MedTodayController {

    @Autowired
    private MedTodayService medTodayService;

    /**
     * 获取当天点位
     * GET /api/v1/med/today
     */
    @Operation(summary = "获取当天点位", description = "获取指定日期的用药点位列表，如果点位不存在会自动生成")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = MedTodayService.TodayRecordsVO.class)))
    })
    @GetMapping
    public Result<MedTodayService.TodayRecordsVO> getTodayRecords(
            @Parameter(description = "日期（可选，默认为今天）", example = "2025-12-18")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        MedTodayService.TodayRecordsVO result = medTodayService.getTodayRecords(date);
        return Result.success(result);
    }

    /**
     * 确保生成点位
     * POST /api/v1/med/today/ensure
     */
    @Operation(summary = "确保生成点位", description = "按计划生成指定日期的records（plan × time × date）并去重")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "生成成功")
    })
    @PostMapping("/ensure")
    public Result<Void> ensureRecords(
            @Parameter(description = "确保生成点位请求参数", required = true)
            @Valid @RequestBody EnsureRecordsRequest request) {
        medTodayService.ensureRecords(request.getDate());
        return Result.success();
    }

    /**
     * 确保生成点位请求
     */
    @Schema(description = "确保生成点位请求参数")
    public static class EnsureRecordsRequest {
        @Schema(description = "日期", example = "2025-12-18", required = true)
        @NotNull(message = "日期不能为空")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate date;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
    }
}

