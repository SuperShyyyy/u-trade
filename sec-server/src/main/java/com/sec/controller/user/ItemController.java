package com.sec.controller.user;


import com.sec.domain.dto.ItemDTO;
import com.sec.domain.vo.ItemVO;
import com.sec.result.PageDTO;
import com.sec.result.Result;
import com.sec.service.IItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 商品表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Api(tags = "商品接口")
@RestController
@RequestMapping("/user/item")
@RequiredArgsConstructor
public class ItemController {
    private final IItemService itemService;


    @GetMapping("/search")
    public Result<PageDTO<ItemVO>> search(@RequestParam String keyword,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(itemService.searchItems(keyword, page, pageSize));
    }


    @ApiOperation("用户上架商品接口")
    @PostMapping
    public Result addItem(@RequestBody ItemDTO itemDTO){
        itemService.addItem(itemDTO);
        return Result.success();
    }

    @ApiOperation("用户下架/重新上架商品接口")
    @PutMapping("/{id}/status")
    public Result updateItemStatus(@PathVariable Long id, @RequestParam Integer status){
        itemService.updateItemStatus(id,status);
        return Result.success();
    }

    @ApiOperation("用户删除商品接口")
    @DeleteMapping("/{id}")
    public Result deleteItem(@PathVariable Long id){
        itemService.deleteItem(id);
        return Result.success();
    }

    @ApiOperation("查询某个商家的已上架商品")
    @GetMapping("/{id}/list")
    public Result<PageDTO<ItemVO>> pageQueryItemsBySellerId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10")  int pageSize
    ){
        PageDTO<ItemVO> dto = itemService.pageQueryItemsBySellerId(id, page, pageSize);
        return Result.success(dto);
    }
}
