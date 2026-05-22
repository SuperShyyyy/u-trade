package com.u.api.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class UserDTO implements Serializable {

    private Long id;
    private String username;
    private String phone;
    private String avatar;
    private Integer creditScore;
    private Integer status;
    private BigDecimal balance;
    private LocalDateTime createTime;

    // getter & setter 省略
}