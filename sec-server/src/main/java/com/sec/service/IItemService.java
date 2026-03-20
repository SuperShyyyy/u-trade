package com.sec.service;

import com.sec.domain.dto.ItemDTO;
import com.sec.domain.po.Item;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sec.domain.vo.ItemVO;
import com.sec.result.PageDTO;

/**
 * <p>
 * 商品表 服务类
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
