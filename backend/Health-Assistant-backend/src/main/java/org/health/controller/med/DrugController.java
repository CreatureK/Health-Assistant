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
import org.health.service.med.DrugService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 药品库控制器
 */
@Tag(name = "药品库管理", description = "药品搜索和详情查询接口")
@RestController
@RequestMapping("/med/drugs")
public class DrugController {

    @Autowired
    private DrugService drugService;

    /**
     * 药品搜索列表
     * GET /api/v1/med/drugs
     */
    @Operation(summary = "药品搜索列表", description = "根据关键词搜索药品")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = DrugService.DrugListVO.class)))
    })
    @GetMapping
    public Result<DrugService.DrugListVO> searchDrugs(
            @Parameter(description = "关键词")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "页码", example = "1")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(required = false) Integer size) {
        DrugService.DrugListVO result = drugService.searchDrugs(keyword, page, size);
        return Result.success(result);
    }

    /**
     * 药品详情
     * GET /api/v1/med/drugs/:id
     */
    @Operation(summary = "药品详情", description = "获取药品的详细信息和说明")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "药品不存在")
    })
    @GetMapping("/{id}")
    public Result<DrugService.DrugDetailVO> getDrugDetail(
            @Parameter(description = "药品ID", required = true)
            @PathVariable Long id) {
        try {
            DrugService.DrugDetailVO result = drugService.getDrugDetail(id);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(ResultCode.NOT_FOUND, e.getMessage());
        }
    }
}

