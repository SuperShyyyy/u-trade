package com.sec.service;

import com.sec.domain.vo.ItemVO;
import com.sec.result.PageDTO;

public interface HomeRecommendService {
    PageDTO<ItemVO> pageQueryRecommendItem(int page, int pageSize);

    void refreshRecommendCache();
}
