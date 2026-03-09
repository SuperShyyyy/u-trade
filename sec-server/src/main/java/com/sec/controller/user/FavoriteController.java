package com.sec.controller.user;


import com.sec.domain.dto.FavoriteDTO;
import com.sec.domain.vo.FavoriteVO;
import com.sec.result.PageDTO;
import com.sec.result.Result;
import com.sec.service.IFavoriteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import springfox.documentation.annotations.ApiIgnore;

import javax.management.ValueExp;

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
@Api(tags = "用户收藏接口")
@RequiredArgsConstructor
public class FavoriteController {
    private final IFavoriteService favoriteService;
    @ApiOperation("分页查询用户收藏")
    @GetMapping("/list")
    public Result<PageDTO<FavoriteVO>> pageQueryFavorite(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ){
        return Result.success(favoriteService.pageQueryFavorite(page,pageSize));
    }

    @ApiOperation("收藏商品")
    @PostMapping
    public Result addFavorite(FavoriteDTO favoriteDTO) {
        favoriteService.addFavorite(favoriteDTO);
        return Result.success();
    }

    @ApiOperation("删除收藏")
    @DeleteMapping
    public Result deleteFavorite(@PathVariable  Long id) {
        favoriteService.deleteFavorite(id);
        return Result.success();
    }
}
