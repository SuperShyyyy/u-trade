package com.u.item.controller;


import com.u.item.domain.vo.CategoryVO;
import com.u.item.domain.vo.ItemVO;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import com.u.item.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 商品分类�?前端控制�?
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@RestController
@RequestMapping("/user/category")
@RequiredArgsConstructor
public class CategoryController {
    private final ICategoryService categoryService;


    @Operation(summary = "查询商品分类")
    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.success(categoryService.listTree());
    }


    @Operation(summary = "根据分类分页查询商品")
    @GetMapping("/{id}/item")
    public Result<PageDTO<ItemVO>> pageQueryItemByCategoryId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
           return Result.success(categoryService.pageQueryItemByCategoryId(id,page,pageSize));

    }
}
