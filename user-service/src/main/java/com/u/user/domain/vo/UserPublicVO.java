package com.u.user.domain.vo;

import lombok.Data;

@Data
public class UserPublicVO {
    private Long id;        // 卖家 ID，用于查询商品
    private String nickname;
    private String avatar;
    private Integer creditScore;
    private Integer status;
}
