package com.u.user.domain.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class UserAddressVO implements Serializable {
    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Integer isDefault;
}
