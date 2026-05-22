package com.u.api.dto.user;

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
    private Integer isDefault;

    // getter & setter 省略
}