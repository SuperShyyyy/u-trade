package com.sec.service;

import com.sec.domain.dto.FavoriteDTO;
import com.sec.domain.po.Favorite;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sec.domain.vo.FavoriteVO;
import com.sec.result.PageDTO;

/**
 * <p>
 * 商品收藏表 服务类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
public interface IFavoriteService extends IService<Favorite> {

    void addFavorite(FavoriteDTO favoriteDTO);

    PageDTO<FavoriteVO> pageQueryFavorite(int page, int pageSize);

    void deleteFavorite(Long id);
}
