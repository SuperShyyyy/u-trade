package com.u.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.u.api.client.user.UserClient;
import com.u.api.dto.user.UserDTO;
import com.u.common.constant.JwtClaimsConstant;
import com.u.common.constant.RedisConstant;
import com.u.common.constant.RoleConstant;
import com.u.admin.domain.dto.AdminDTO;
import com.u.admin.domain.dto.LoginDTO;
import com.u.admin.domain.po.Admin;
import com.u.admin.domain.vo.AdminVO;
import com.u.admin.domain.vo.LoginVO;
import com.u.admin.mapper.AdminMapper;
import com.u.admin.service.IAdminService;
import com.u.common.context.BaseContext;
import com.u.common.exception.BusinessException;
import com.u.common.exception.PermissionDeniedException;
import com.u.common.properties.JwtProperties;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import com.u.common.result.ResultCode;
import com.u.common.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 系统管理员表 服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {

    private final JwtProperties jwtProperties;
    private final UserClient userClient;
    private final StringRedisTemplate stringRedisTemplate;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String LOGIN_FAIL_PREFIX = "admin:login:fail:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    @Override
    public LoginVO adminLogin(LoginDTO loginDTO) {
        // 登录限流：检查是否被临时锁定
        String failKey = LOGIN_FAIL_PREFIX + loginDTO.getUsername();
        String failCountStr = stringRedisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr != null ? Integer.parseInt(failCountStr) : 0;
        if (failCount >= MAX_LOGIN_ATTEMPTS) {
            throw new BusinessException("登录失败次数过多，账号已被临时锁定，请" + LOCK_DURATION_MINUTES + "分钟后重试");
        }
        Admin admin = lambdaQuery()
                .eq(Admin::getUsername, loginDTO.getUsername())
                .one();

        if (admin == null) {
            throw new BusinessException("管理员账号不存在");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), admin.getPassword())) {
            // 登录失败，增加失败计数
            stringRedisTemplate.opsForValue().set(failKey, String.valueOf(failCount + 1),
                    LOCK_DURATION_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
            throw new BusinessException("密码错误");
        }

        // 登录成功，清除失败计数
        stringRedisTemplate.delete(failKey);

        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        Integer dbRole = admin.getRole();
        String roleStr = dbRole == 2 ? RoleConstant.SUPER_ADMIN : RoleConstant.COMMON_ADMIN;

        // 获取/初始化 tokenVersion
        String versionKey = RedisConstant.TOKEN_VERSION_KEY + admin.getId();
        String v = stringRedisTemplate.opsForValue().get(versionKey);
        int tokenVersion = (v != null) ? Integer.parseInt(v) : 0;
        if (v == null) {
            stringRedisTemplate.opsForValue().set(versionKey, "0",
                    jwtProperties.getAdminTtl(), TimeUnit.MILLISECONDS);
        }

        // 生成 sessionId，单设备登录互踢
        String sessionId = UUID.randomUUID().toString();
        String sessionKey = RedisConstant.USER_SESSION_KEY + admin.getId();
        stringRedisTemplate.opsForValue().set(sessionKey, sessionId,
                jwtProperties.getAdminTtl(), TimeUnit.MILLISECONDS);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.CURRENT_ID, admin.getId());
        claims.put(JwtClaimsConstant.ROLE, roleStr);
        claims.put(JwtClaimsConstant.SOURCE_TYPE, "ADMIN");
        claims.put(JwtClaimsConstant.SESSION_ID, sessionId);
        claims.put(JwtClaimsConstant.TOKEN_VERSION, tokenVersion);

        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims
        );

        log.info("管理员登录成功, adminId={}, sessionId={}, tokenVersion={}",
                admin.getId(), sessionId, tokenVersion);

        return LoginVO.builder()
                .id(admin.getId())
                .token(token)
                .role(roleStr)
                .sourceType("ADMIN")
                .build();
    }

    @Override
    public PageDTO<AdminVO> pageQuery(int page, int pageSize) {
        String role = BaseContext.getCurrentRole();
        if (!RoleConstant.SUPER_ADMIN.equals(role)) {
            throw new PermissionDeniedException("权限不足");
        }
        Page<Admin> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                Admin::getId,
                Admin::getUsername,
                Admin::getNickname,
                Admin::getRole,
                Admin::getStatus,
                Admin::getLastLoginTime,
                Admin::getLastLoginIp,
                Admin::getAvatar,
                Admin::getCreateTime
        ).orderByDesc(Admin::getCreateTime);
        IPage<Admin> adminIPage = this.page(pageParam, queryWrapper);
        IPage<AdminVO> voPage = adminIPage.convert(admin -> {
            AdminVO vo = new AdminVO();
            BeanUtils.copyProperties(admin, vo);
            return vo;
        });
        return PageDTO.of(voPage);
    }

    @Override
    public void saveAdmin(AdminDTO adminDTO){
        String currentRole = BaseContext.getCurrentRole();
        if (! RoleConstant.SUPER_ADMIN.equals(currentRole)) {
            throw new PermissionDeniedException("权限不足，仅超级管理员可创建账号");
        }
        Long count = lambdaQuery()
                .eq(Admin::getUsername, adminDTO.getUsername())
                .count();
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        Admin admin = new Admin();
        BeanUtils.copyProperties(adminDTO, admin);
        admin.setPassword(passwordEncoder.encode(adminDTO.getPassword()));
        admin.setStatus(1);
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());

        this.save(admin);
    }

    @Override
    public void updateAdminStatus(Long id, Integer status) {
        String currentRole = BaseContext.getCurrentRole();
        if ( ! RoleConstant.SUPER_ADMIN.equals(currentRole)) {
            throw new PermissionDeniedException("权限不足，无法执行删除");
        }
        if (id == null) {
            throw new BusinessException("账号ID不能为空");
        }
        if (id.equals(BaseContext.getCurrentId())) {
            throw new BusinessException("禁止修改当前登录账号的状态");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("非法状态参数");
        }
        boolean successful = lambdaUpdate()
                .eq(Admin::getId, id)
                .set(Admin::getStatus, status)
                .update();
        if (!successful) {
            throw new BusinessException("修改失败 账号不存在");
        }
    }

    @Override
    public void deleteAdminById(Long adminId) {
        String currentRole = BaseContext.getCurrentRole();
        if (!RoleConstant.SUPER_ADMIN.equals(currentRole)) {
            throw new PermissionDeniedException("权限不足，无法执行删除");
        }
        if (adminId == null) {
            throw new BusinessException("操作失败 缺少目标ID");
        }
        Long currentId = BaseContext.getCurrentId();
        if (adminId.equals(currentId)) {
            throw new BusinessException("禁止删除当前登录的管理员账号");
        }
        Admin admin = this.getById(adminId);
        if (admin == null) {
            throw new BusinessException("删除失败，目标账号不存在");
        }
        boolean success = this.removeById(adminId);
        if (!success) {
            throw new BusinessException("删除失败");
        }
    }

    @Override
    public PageDTO<UserDTO> pageQueryUser(int page, int pageSize){
        String currentRole = BaseContext.getCurrentRole();
        if (!RoleConstant.COMMON_ADMIN.equals(currentRole) && !RoleConstant.SUPER_ADMIN.equals(currentRole)) {
            throw new PermissionDeniedException("权限不足，无法查找");
        }
        Result<PageDTO<UserDTO>> result = userClient.pageQuery(page, pageSize);
        ensureSuccess(result, "查询用户失败");
        if (result.getData() == null) {
            throw new BusinessException("用户服务返回为空");
        }
        return result.getData();
    }
    @Override
    public void updateUserStatus(Long id, Integer status) {
        if (id == null || status == null) {
            throw new BusinessException("参数异常无法查询");
        }
        String currentRole = BaseContext.getCurrentRole();
        if (!RoleConstant.COMMON_ADMIN.equals(currentRole) && !RoleConstant.SUPER_ADMIN.equals(currentRole)) {
            throw new PermissionDeniedException("权限不足，无法查找");
        }
        Result<Void> updateUserResult = userClient.updateStatus(id, status);
        ensureSuccess(updateUserResult, "更新用户状态失败");
    }

    @Override
    public void resetUserPassword(Long userId, String newPassword) {
        String currentRole = BaseContext.getCurrentRole();
        if (!RoleConstant.COMMON_ADMIN.equals(currentRole) && !RoleConstant.SUPER_ADMIN.equals(currentRole)) {
            throw new PermissionDeniedException("权限不足，无法重置用户密码");
        }
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BusinessException("新密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 32) {
            throw new BusinessException("新密码长度需在6-32位之间");
        }
        Result<Void> result = userClient.resetPassword(userId, newPassword);
        ensureSuccess(result, "重置用户密码失败");
    }

    private void ensureSuccess(Result<?> result, String defaultErrorMsg) {
        if (result == null) {
            throw new BusinessException(defaultErrorMsg);
        }
        if (!ResultCode.SUCCESS.equals(result.getCode())) {
            String message = result.getMessage() == null ? defaultErrorMsg : result.getMessage();
            throw new BusinessException(message);
        }
    }

    @Override
    public void logout(Long adminId) {
        incrTokenVersion(adminId);
        stringRedisTemplate.delete(RedisConstant.USER_SESSION_KEY + adminId);
    }

    private void incrTokenVersion(Long adminId) {
        String key = RedisConstant.TOKEN_VERSION_KEY + adminId;
        long ttl = jwtProperties.getAdminTtl();
        try {
            Long newVersion = stringRedisTemplate.opsForValue().increment(key);
            if (newVersion != null) {
                stringRedisTemplate.expire(key, ttl, TimeUnit.MILLISECONDS);
            }
            log.info("Admin tokenVersion 递增成功, adminId={}, newVersion={}", adminId, newVersion);
        } catch (Exception e) {
            log.error("Admin tokenVersion 递增失败 (Redis异常), adminId={}", adminId, e);
        }
    }
}
