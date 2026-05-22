package com.u.item.service;

import com.u.item.domain.vo.ItemVO;
import com.u.common.result.PageDTO;

public interface HomeRecommendService {
    PageDTO<ItemVO> pageQueryRecommendItem(int page, int pageSize);

    void refreshRecommendCache();
}
