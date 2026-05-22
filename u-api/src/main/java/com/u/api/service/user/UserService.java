package com.u.api.service.user;

import com.u.api.client.user.UserClient;
import com.u.api.dto.user.UserDTO;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserClient userClient;

    public UserService(UserClient userClient) {
        this.userClient = userClient;
    }

    public PageDTO<UserDTO> pageQuery(int page, int pageSize) {
        Result<PageDTO<UserDTO>> result = userClient.pageQuery(page, pageSize);
        if (result.success().getData() != null) {
            return result.getData();
        } else {
            throw new RuntimeException("查询用户失败: " + result.getMessage());
        }
    }

    public void updateStatus(Long id, Integer status) {
        userClient.updateStatus(id, status);
    }
}