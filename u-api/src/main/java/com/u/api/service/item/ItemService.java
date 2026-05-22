package com.u.api.service.item;

import com.u.api.client.item.ItemClient;
import com.u.api.dto.item.OrderItemDTO;
import com.u.common.result.Result;
import org.springframework.stereotype.Service;

@Service
public class ItemService {

    private final ItemClient itemClient;

    public ItemService(ItemClient itemClient) {
        this.itemClient = itemClient;
    }

    public OrderItemDTO getOrderItem(Long id) {
        Result<OrderItemDTO> result = itemClient.getOrderItem(id);
        if (result.success().getData() != null) {
            return result.getData();
        } else {
            throw new RuntimeException("获取商品失败: " + result.getMessage());
        }
    }

    public void lockItem(Long id) {
        itemClient.lockItem(id);
    }

    public void markItemSold(Long id) {
        itemClient.markItemSold(id);
    }

    public void releaseItem(Long id) {
        itemClient.releaseItem(id);
    }
}