package com.u.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.u.api.client.user.UserClient;
import com.u.api.dto.user.UserDTO;
import com.u.common.constant.JwtClaimsConstant;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统管理员表 服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {

    private final JwtProperties jwtProperties;
    private final UserClient userClient;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO adminLogin(LoginDTO loginDTO) {
        Admin admin = lambdaQuery()
                .eq(Admin::getUsername, loginDTO.getUsername())
                .one();

        if (admin == null) {
            throw new BusinessException("管理员账号不存在");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), admin.getPassword())) {
            throw new BusinessException("密码错误");
        }

        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        Integer dbRole = admin.getRole();
        String roleStr;

        if (dbRole == 2) {
            roleStr = RoleConstant.SUPER_ADMIN;
        } else {
            roleStr = RoleConstant.COMMON_ADMIN;
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.CURRENT_ID, admin.getId());
        claims.put(JwtClaimsConstant.ROLE, roleStr);
        claims.put(JwtClaimsConstant.SOURCE_TYPE, "ADMIN");

        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims
        );
        log.info("当前使用的密钥: {}", jwtProperties.getAdminSecretKey());

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

    private void ensureSuccess(Result<?> result, String defaultErrorMsg) {
        if (result == null) {
            throw new BusinessException(defaultErrorMsg);
        }
        if (!ResultCode.SUCCESS.equals(result.getCode())) {
            String message = result.getMessage() == null ? defaultErrorMsg : result.getMessage();
            throw new BusinessException(message);
        }
    }
}
