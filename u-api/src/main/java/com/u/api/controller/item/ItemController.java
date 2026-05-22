package com.u.api.controller.item;

import com.u.api.dto.item.OrderItemDTO;
import com.u.api.service.item.ItemService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/{id}")
    public OrderItemDTO getOrderItem(@PathVariable Long id) {
        return itemService.getOrderItem(id);
    }

    @PutMapping("/{id}/lock")
    public void lockItem(@PathVariable Long id) {
        itemService.lockItem(id);
    }

    @PutMapping("/{id}/sold")
    public void markItemSold(@PathVariable Long id) {
        itemService.markItemSold(id);
    }

    @PutMapping("/{id}/release")
    public void releaseItem(@PathVariable Long id) {
        itemService.releaseItem(id);
    }
}