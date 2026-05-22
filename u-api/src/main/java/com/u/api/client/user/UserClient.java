package com.u.api.client.user;

import com.u.api.dto.user.UserDTO;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", path = "/inner/admin/users")
public interface UserClient {

    @GetMapping("/page")
    Result<PageDTO<UserDTO>> pageQuery(@RequestParam("page") int page,
                                       @RequestParam("pageSize") int pageSize);

    @PutMapping("/{id}/status")
    Result<Void> updateStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status);
}