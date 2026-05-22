package com.u.api.dto.item;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemDTO implements Serializable {

    private Long id;
    private Long sellerId;
    private String title;
    private String description;
    private BigDecimal price;
    private String cover;
    private List<String> images;
    private Integer isFreeShipping;
    private BigDecimal shippingFee;

}