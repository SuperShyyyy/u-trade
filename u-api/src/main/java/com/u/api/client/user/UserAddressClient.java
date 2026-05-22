package com.u.api.client.user;

import com.u.api.dto.user.UserAddressDTO;
import com.u.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", path = "/inner/order/addresses")
public interface UserAddressClient {

    @GetMapping
    Result<UserAddressDTO> getAddress(@RequestParam("id") Long id, @RequestParam("userId") Long userId);
}