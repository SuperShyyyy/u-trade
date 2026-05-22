package com.u.item.controller;

import com.u.item.domain.vo.ItemVO;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import com.u.item.service.HomeRecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
@Tag(name = "首页推荐接口")
public class HomeController {
    private final HomeRecommendService homeRecommendService;

    @GetMapping("/recommend")
    @Operation(summary = "获取首页推荐")
    public Result<PageDTO<ItemVO>> pageQueryRecommendItem(
           @RequestParam(defaultValue = "1") int page,
           @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(homeRecommendService.pageQueryRecommendItem(page,pageSize));
    }
}
