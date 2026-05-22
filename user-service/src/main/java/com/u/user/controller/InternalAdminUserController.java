package com.u.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.u.api.dto.user.UserDTO;
import com.u.api.internal.user.InternalAdminUserApi;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import com.u.user.domain.po.User;
import com.u.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class InternalAdminUserController implements InternalAdminUserApi {

    private final IUserService userService;

    @Override
    public Result<PageDTO<UserDTO>> pageQuery(int page, int pageSize) {
        // 分页参数
        Page<User> pageParam = new Page<>(page, pageSize);

        // 查询条件
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                        User::getId,
                        User::getUsername,
                        User::getPhone,
                        User::getAvatar,
                        User::getCreditScore,
                        User::getStatus,
                        User::getCreateTime
                )
                .orderByDesc(User::getCreateTime);

        // 执行查询
        IPage<User> userPage = userService.page(pageParam, queryWrapper);

        // 转换为 DTO
        IPage<UserDTO> dtoPage = userPage.convert(user -> {
            UserDTO dto = new UserDTO();
            BeanUtils.copyProperties(user, dto);
            return dto;
        });

        // 返回包装好的分页结果
        return Result.success(PageDTO.of(dtoPage));
    }

    @Override
    public Result<Void> updateStatus(Long id, Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userService.updateById(user);
        return Result.success();
    }

    @Override
    public Result<Map<Long, Integer>> getUserCreditScores(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Result.success(Map.of());
        }

        List<User> users = userService.lambdaQuery()
                .in(User::getId, userIds)
                .select(User::getId, User::getCreditScore)
                .list();
        Map<Long, Integer> creditScores = users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> user.getCreditScore() == null ? 0 : user.getCreditScore()
                ));
        return Result.success(creditScores);
    }
}