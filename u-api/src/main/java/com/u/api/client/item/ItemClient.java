package com.u.api.client.item;

import com.u.api.dto.item.OrderItemDTO;
import com.u.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "item-service", path = "/inner/order/items")
public interface ItemClient {

    @GetMapping("/{id}")
    Result<OrderItemDTO> getOrderItem(@PathVariable("id") Long id);

    @PutMapping("/{id}/lock")
    Result<Void> lockItem(@PathVariable("id") Long id);

    @PutMapping("/{id}/sold")
    Result<Void> markItemSold(@PathVariable("id") Long id);

    @PutMapping("/{id}/release")
    Result<Void> releaseItem(@PathVariable("id") Long id);
}