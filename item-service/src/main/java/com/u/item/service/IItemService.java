package com.u.item.service;

import com.u.item.domain.dto.ItemDTO;
import com.u.item.domain.po.Item;
import com.baomidou.mybatisplus.extension.service.IService;
import com.u.item.domain.vo.ItemVO;
import com.u.common.result.PageDTO;

/**
 * <p>
 * 商品�?服务�?
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
public interface IItemService extends IService<Item> {
    void updateItemStatus(Long id, Integer status);

    void addItem(ItemDTO itemDTO);

    void deleteItem(Long id);

    PageDTO<ItemVO> pageQueryItemsBySellerId(Long id,int page,int pageSize);

    PageDTO<ItemVO> searchItems(String keyword, int page, int pageSize);

    ItemVO getItemById(Long id);
}
