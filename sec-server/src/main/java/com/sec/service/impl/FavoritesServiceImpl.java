package com.sec.service.impl;

import com.sec.domain.po.Favorite;
import com.sec.mapper.FavoriteMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sec.service.IFavoriteService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商品收藏表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Service
public class FavoritesServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements IFavoriteService {

}
