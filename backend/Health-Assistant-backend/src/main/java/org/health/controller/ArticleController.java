package org.health.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.health.common.Result;
import org.health.common.ResultCode;
import org.health.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 健康文章控制器
 */
@Tag(name = "健康文章", description = "健康文章相关接口")
@RestController
@RequestMapping("/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    /**
     * 文章列表
     * GET /api/v1/articles
     */
    @Operation(summary = "文章列表", description = "获取健康文章列表，支持分类、关键词搜索和分页查询")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ArticleService.ArticleListVO.class)))
    })
    @GetMapping
    public Result<ArticleService.ArticleListVO> getArticleList(
            @Parameter(description = "分类（可选：养生保健、慢病管理、运动与康复、心理健康与社交）", example = "养生保健")
            @RequestParam(required = false) String category,
            @Parameter(description = "关键词（可选，用于搜索标题和内容）", example = "健康")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "页码（可选，默认为1）", example = "1")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "每页大小（可选，默认为20）", example = "20")
            @RequestParam(required = false) Integer size) {
        ArticleService.ArticleListVO result = articleService.getArticleList(category, keyword, page, size);
        return Result.success(result);
    }

    /**
     * 文章详情
     * GET /api/v1/articles/:id
     */
    @Operation(summary = "文章详情", description = "获取健康文章详情，访问时自动增加浏览次数")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ArticleService.ArticleDetailVO.class))),
            @ApiResponse(responseCode = "404", description = "文章不存在")
    })
    @GetMapping("/{id}")
    public Result<ArticleService.ArticleDetailVO> getArticleDetail(
            @Parameter(description = "文章ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            ArticleService.ArticleDetailVO result = articleService.getArticleDetail(id);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(ResultCode.NOT_FOUND, e.getMessage());
        }
    }
}

