package com.u.api.controller.user;

import com.u.api.dto.user.UserDTO;
import com.u.api.service.user.UserService;
import com.u.common.result.PageDTO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/page")
    public PageDTO<UserDTO> pageQuery(@RequestParam int page, @RequestParam int pageSize) {
        return userService.pageQuery(page, pageSize);
    }

    @PutMapping("/{id}/status")
    public void updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
    }
}