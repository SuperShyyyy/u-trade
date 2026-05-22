package com.u.api.client.user;

import com.u.api.dto.user.UserDTO;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", path = "/inner/admin/users")
public interface UserClient {

    @GetMapping("/page")
    Result<PageDTO<UserDTO>> pageQuery(@RequestParam("page") int page,
                                       @RequestParam("pageSize") int pageSize);

    @PostMapping("/status")
    Result<Void> updateStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status);

    @GetMapping("/credit-scores")
    Result<Map<Long, Integer>> getUserCreditScores(@RequestParam("userIds") List<Long> userIds);
}