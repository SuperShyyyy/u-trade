package com.u.item.controller;


import com.u.item.domain.dto.ItemDTO;
import com.u.item.domain.vo.ItemVO;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import com.u.item.service.IItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商品接口")
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


    @Operation(summary = "用户上架商品接口")
    @PostMapping
    public Result addItem(@RequestBody ItemDTO itemDTO){
        itemService.addItem(itemDTO);
        return Result.success();
    }

    @Operation(summary = "用户下架/重新上架商品接口")
    @PutMapping("/{id}/status")
    public Result updateItemStatus(@PathVariable Long id, @RequestParam Integer status){
        itemService.updateItemStatus(id,status);
        return Result.success();
    }

    @Operation(summary = "用户删除商品接口")
    @DeleteMapping("/{id}")
    public Result deleteItem(@PathVariable Long id){
        itemService.deleteItem(id);
        return Result.success();
    }

    @Operation(summary = "查询某个商家的已上架商品")
    @GetMapping("/{id}/list")
    public Result<PageDTO<ItemVO>> pageQueryItemsBySellerId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10")  int pageSize
    ){
        PageDTO<ItemVO> dto = itemService.pageQueryItemsBySellerId(id, page, pageSize);
        return Result.success(dto);
    }

    @Operation(summary = "通过id查询单个商品详情")
    @GetMapping("/{id}")
    public Result<ItemVO> getItemById(@PathVariable Long id){
        return Result.success(itemService.getItemById(id));
    }
}
