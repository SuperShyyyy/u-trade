package com.sec.service.impl;

import com.sec.domain.po.Review;
import com.sec.mapper.ReviewMapper;
import com.sec.service.IReviewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 评价表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-08
 */
@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements IReviewService {

}
