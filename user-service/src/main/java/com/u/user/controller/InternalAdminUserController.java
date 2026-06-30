package com.u.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.u.api.dto.user.UserDTO;
import com.u.api.internal.user.InternalAdminUserApi;
import com.u.common.constant.RedisConstant;
import com.u.common.properties.JwtProperties;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import com.u.user.domain.po.User;
import com.u.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inner/admin/users")
@RequiredArgsConstructor
@Slf4j
public class InternalAdminUserController implements InternalAdminUserApi {

    private final IUserService userService;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

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
        // 乐观锁：先查询获取 version，再更新
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setStatus(status);
        userService.updateById(user);

        // 封禁用户时：写 ban 标记 + 递增 tokenVersion（所有旧 Token 立即失效）
        if (status != null && status == 0) {
            long ttlMillis = jwtProperties.getUserTtl();
            stringRedisTemplate.opsForValue().set(RedisConstant.USER_BAN_KEY + id, "1", ttlMillis, TimeUnit.MILLISECONDS);
            stringRedisTemplate.opsForValue().increment(RedisConstant.TOKEN_VERSION_KEY + id);
            stringRedisTemplate.expire(RedisConstant.TOKEN_VERSION_KEY + id, ttlMillis, TimeUnit.MILLISECONDS);
        } else if (status != null && status == 1) {
            stringRedisTemplate.delete(RedisConstant.USER_BAN_KEY + id);
        }
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

    @Override
    public Result<Void> resetPassword(Long userId, String newPassword) {
        userService.resetPassword(userId, newPassword);
        // UserServiceImpl.resetPassword 已调用 incrTokenVersion，所有 Token 自动失效
        return Result.success();
    }
}