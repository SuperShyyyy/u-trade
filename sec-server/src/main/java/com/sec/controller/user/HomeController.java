package com.sec.controller.user;

import com.sec.domain.vo.ItemVO;
import com.sec.result.PageDTO;
import com.sec.result.Result;
import com.sec.service.HomeRecommendService;
import io.opentracing.tag.Tags;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
@Api(tags = "首页推荐接口")
public class HomeController {
    private final HomeRecommendService homeRecommendService;

    @GetMapping("/recommend")
    @ApiOperation("获取首页推荐")
    public Result<PageDTO<ItemVO>> pageQueryRecommendItem(
           @RequestParam(defaultValue = "1") int page,
           @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(homeRecommendService.pageQueryRecommendItem(page,pageSize));
    }
}
