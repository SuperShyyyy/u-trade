package com.sec.controller.user;


import com.sec.domain.vo.CategoryVO;
import com.sec.domain.vo.ItemVO;
import com.sec.result.PageDTO;
import com.sec.result.Result;
import com.sec.service.ICategoryService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 商品分类表 前端控制器
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


    @ApiOperation("查询商品分类")
    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.success(categoryService.listTree());
    }


    @ApiOperation("根据分类分页查询商品")//分类id
    @GetMapping("/{id}/item")
    public Result<PageDTO<ItemVO>> pageQueryItemByCategoryId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
           return Result.success(categoryService.pageQueryItemByCategoryId(id,page,pageSize));

    }
}
