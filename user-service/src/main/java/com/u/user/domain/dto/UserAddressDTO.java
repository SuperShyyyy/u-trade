package com.u.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.io.Serializable;

@Data
public class UserAddressDTO implements Serializable {
    private Long id;

    @NotBlank(message = "收件人姓名不能为空")
    private String receiverName;

    @NotBlank(message = "收件人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "区县不能为空")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

    private Integer isDefault; // 1 默认，0 非默认

    public void setId(Long id) {
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalArgumentException("禁止修改已有主键 ID");
        }
        this.id = id;
    }
}
