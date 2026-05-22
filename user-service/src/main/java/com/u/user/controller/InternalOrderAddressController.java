package com.u.user.controller;

import com.u.api.dto.user.UserAddressDTO;
import com.u.api.internal.user.InternalOrderAddressApi;
import com.u.common.exception.BusinessException;
import com.u.common.result.Result;
import com.u.user.domain.po.UserAddress;
import com.u.user.service.IUserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inner/order/addresses")
@RequiredArgsConstructor
public class InternalOrderAddressController implements InternalOrderAddressApi {

    private final IUserAddressService userAddressService;

    @Override
    public Result<UserAddressDTO> getAddress(Long id, Long userId) {
        UserAddress address = userAddressService.lambdaQuery()
                .eq(UserAddress::getId, id)
                .eq(UserAddress::getUserId, userId)
                .one();
        if (address == null) {
            throw new BusinessException("收货地址不存在");
        }

        UserAddressDTO vo = new UserAddressDTO();
        BeanUtils.copyProperties(address, vo);
        return Result.success(vo);
    }
}
