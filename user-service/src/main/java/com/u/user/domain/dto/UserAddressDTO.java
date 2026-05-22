package com.u.user.domain.dto;

import lombok.Data;
import java.io.Serializable;

@Data

public class UserAddressDTO implements Serializable {
    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Integer isDefault; // 1 默认，0 非默认
    public void setId(Long id) {
        if (this.id != null && !this.id.equals(id)) {
            // 记录日志或抛出异常 阻止非法修改行为
            throw new IllegalArgumentException("禁止修改已有主键 ID");
        }
        this.id = id;
    }
}
