package com.u.user.controller;

import com.u.user.domain.vo.FavoriteVO;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import com.u.user.service.IFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 商品收藏表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@RestController
@RequestMapping("/user/favorite")
@Tag(name = "用户收藏接口")
@RequiredArgsConstructor
public class FavoriteController {
    private final IFavoriteService favoriteService;
    @Operation(summary = "分页查询用户收藏")
    @GetMapping("/list")
    public Result<PageDTO<FavoriteVO>> pageQueryFavorite(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ){
        return Result.success(favoriteService.pageQueryFavorite(page,pageSize));
    }

    @Operation(summary = "收藏商品")
    @PostMapping("/add")
    public Result addFavorite(@RequestParam("itemId") Long itemId) {
        favoriteService.addFavorite(itemId);
        return Result.success();
    }

    @Operation(summary = "删除收藏")
    @DeleteMapping("{itemId}")
    public Result deleteFavorite(@PathVariable Long itemId) {
        favoriteService.deleteFavorite(itemId);
        return Result.success();
    }
}
