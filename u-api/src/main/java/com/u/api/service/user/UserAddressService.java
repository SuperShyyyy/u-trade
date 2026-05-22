package com.u.api.service.user;

import com.u.api.client.user.UserAddressClient;
import com.u.api.dto.user.UserAddressDTO;
import com.u.common.result.Result;
import org.springframework.stereotype.Service;

@Service
public class UserAddressService {

    private final UserAddressClient addressClient;

    public UserAddressService(UserAddressClient addressClient) {
        this.addressClient = addressClient;
    }

    public UserAddressDTO getAddress(Long id, Long userId) {
        Result<UserAddressDTO> result = addressClient.getAddress(id, userId);
        if (result.success().getData() != null) {
            return result.getData();
        } else {
            throw new RuntimeException("查询用户地址失败: " + result.getMessage());
        }
    }
}