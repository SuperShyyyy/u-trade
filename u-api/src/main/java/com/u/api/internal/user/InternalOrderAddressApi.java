package com.u.api.internal.user;

import com.u.api.dto.user.UserAddressDTO;
import com.u.common.result.Result;
import org.springframework.web.bind.annotation.*;

/**
 * 内部订单服务调用用户收货地址接口
 */
@RequestMapping("/inner/order/addresses")
public interface InternalOrderAddressApi {

    /**
     * 根据用户ID和地址ID查询收货地址
     *
     * @param id     地址ID
     * @param userId 用户ID
     * @return 收货地址信息
     */
    @GetMapping
    Result<UserAddressDTO> getAddress(
            @RequestParam("id") Long id,
            @RequestParam("userId") Long userId
    );
}