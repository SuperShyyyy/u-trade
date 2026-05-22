package com.u.user.service;

import com.u.user.domain.po.Favorite;
import com.baomidou.mybatisplus.extension.service.IService;
import com.u.user.domain.vo.FavoriteVO;
import com.u.common.result.PageDTO;

/**
 * <p>
 * 商品收藏表 服务类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
public interface IFavoriteService extends IService<Favorite> {

    void addFavorite(Long itemId);

    PageDTO<FavoriteVO> pageQueryFavorite(int page, int pageSize);

    void deleteFavorite(Long id);
}
