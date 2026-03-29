package com.sec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sec.constant.*;
import com.sec.context.BaseContext;
import com.sec.domain.dto.AdminDTO;
import com.sec.domain.dto.LoginDTO;
import com.sec.domain.po.Admin;
import com.sec.domain.po.Item;
import com.sec.domain.po.User;
import com.sec.domain.vo.AdminVO;
import com.sec.domain.vo.LoginVO;
import com.sec.domain.vo.UserVO;
import com.sec.exception.BusinessException;
import com.sec.exception.PermissionDeniedException;
import com.sec.mapper.AdminMapper;
import com.sec.mapper.ItemMapper;
import com.sec.mapper.UserMapper;
import com.sec.mq.sender.ItemEsSender;
import com.sec.properties.JwtProperties;
import com.sec.result.PageDTO;
import com.sec.result.Result;
import com.sec.service.IAdminService;
import com.sec.service.ItemEsService;
import com.sec.utils.JwtUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统管理员表 服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor // 自动生成构造函数，注入所有 final 字段
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {

    // 配置属性
    private final JwtProperties jwtProperties;

    // 密码编码器
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final UserMapper usersMapper;
    private final ItemMapper itemMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ItemEsSender itemEsSender;
    @Override
    public LoginVO adminLogin(LoginDTO loginDTO) {
        // 1. 根据账号查询管理员
        Admin admin = lambdaQuery()
                .eq(Admin::getUsername, loginDTO.getUsername())
                .one();

        // 2. 校验管理员是否存在
        if (admin == null) {
            throw new BusinessException("管理员账号不存在");
        }

        // 3. 校验密码 (使用 BCrypt 安全比对)
        // matches(前端传来的明文, 数据库存的密文)
        if (!passwordEncoder.matches(loginDTO.getPassword(), admin.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 4. 检查账号状态 (0=禁用)
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 5. 角色映射逻辑
        Integer dbRole = admin.getRole();
        String roleStr;

        if (dbRole == 2) {
            roleStr = RoleConstant.SUPER_ADMIN; // 超级管理员
        } else {
            // 兜底策略：默认视为普通管理员 (包括 dbRole==1 或 null 的情况)
            roleStr = RoleConstant.COMMON_ADMIN;
        }

        // 6. 构造 JWT Claims
        Map<String, Object> claims = new HashMap<>();

        // 统一 ID 字段
        claims.put(JwtClaimsConstant.CURRENT_ID, admin.getId());

        // 角色标识 (COMMON_ADMIN / SUPER_ADMIN)
        claims.put(JwtClaimsConstant.ROLE, roleStr);

        // 来源标识
        claims.put(JwtClaimsConstant.SOURCE_TYPE, "ADMIN");

        // 7. 生成 Token
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(), // 管理员专属密钥 (与 User 不同)
                jwtProperties.getAdminTtl(),       // 管理员 Token 有效期
                claims
        );
        log.info("当前使用的密钥: {}", jwtProperties.getAdminSecretKey());
        // 8. 返回 VO
        return LoginVO.builder()
                .id(admin.getId())
                .token(token)
                .role(roleStr)
                .sourceType("ADMIN")
                .build();
    }

    @Override
    public PageDTO<AdminVO> pageQuery(int page, int pageSize) {
        // 1 权限校验
        String role = BaseContext.getCurrentRole();
        if (!RoleConstant.SUPER_ADMIN.equals(role)) {
            throw new PermissionDeniedException("权限不足");
        }
        // 2 构建分页对象
        Page<Admin> pageParam = new Page<>(page, pageSize);
        // 3 构建查询条件
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
        // 4 执行查询
        IPage<Admin> adminIPage = this.page(pageParam, queryWrapper);
        IPage<AdminVO> voPage = adminIPage.convert(admin -> {
            AdminVO vo = new AdminVO();
            BeanUtils.copyProperties(admin, vo);
            return vo;
        });
        // 6 调用静态方法返回
        return PageDTO.of(voPage);
    }

    @Override
    public void saveAdmin(AdminDTO adminDTO){
        String currentRole = BaseContext.getCurrentRole();
        if (! RoleConstant.SUPER_ADMIN.equals(currentRole)) {
            throw new PermissionDeniedException("权限不足，仅超级管理员可创建账号");
        }
        // 2. 检查账号是否已存在
        Long count = lambdaQuery()
                .eq(Admin::getUsername, adminDTO.getUsername())
                .count();
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        Admin admin = new Admin();
        BeanUtils.copyProperties(adminDTO, admin);
        // 4. 密码加密
        admin.setPassword(passwordEncoder.encode(adminDTO.getPassword()));
        // 5. 设置默认状态与时间
        admin.setStatus(1);
        // 设置时间
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());

        // 6. 写入数据库
        this.save(admin);
    }
    @Override
    public void updateAdminStatus(Long id, Integer status) {
        // 1 身份校验
        String currentRole = BaseContext.getCurrentRole();
        if ( ! RoleConstant.SUPER_ADMIN.equals(currentRole)) {
            throw new PermissionDeniedException("权限不足，无法执行删除");
        }
        // 2 目标非空校验
        if (id == null) {
            throw new BusinessException("账号ID不能为空");
        }
        // 3 禁止修改自身状态
        if (id.equals(BaseContext.getCurrentId())) {
            throw new BusinessException("禁止修改当前登录账号的状态");
        }
        // 4 参数范围校验
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("非法状态参数");
        }
        // 5 执行更新
        boolean successful = lambdaUpdate()
                .eq(Admin::getId, id)
                .set(Admin::getStatus, status)
                .update();
        if (!successful) {
            throw new BusinessException("修改失败 账号不存在");
        }
    };

    @Override
    public void deleteAdminById(Long adminId) {
        // 1 仅超级管理员可操作
        String currentRole = BaseContext.getCurrentRole();
        if (!RoleConstant.SUPER_ADMIN.equals(currentRole)) {
            throw new PermissionDeniedException("权限不足，无法执行删除");
        }
        // 2 参数校验
        if (adminId == null) {
            throw new BusinessException("操作失败 缺少目标ID");
        }
        //  禁止删除当前在线的自己
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
    public PageDTO<UserVO> pageQueryUser(int page, int pageSize){
        String currentRole = BaseContext.getCurrentRole();
        if (!RoleConstant.COMMON_ADMIN.equals(currentRole) && !RoleConstant.SUPER_ADMIN.equals(currentRole)) {
            throw new PermissionDeniedException("权限不足，无法查找");
        }
        Page<User> pageParam = new Page<>(page, pageSize);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                User::getId,
                User::getUsername,
                User::getNickname,
                User::getPhone,
                User::getStatus,
                User::getAvatar,
                User::getCreditScore,
                User::getCreateTime
        ).orderByDesc(User::getCreateTime);

        IPage<User> usersPage = usersMapper.selectPage(pageParam, queryWrapper);

        IPage<UserVO> voPage = usersPage.convert(user -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            return vo;
        });

        return PageDTO.of(voPage);
    }


    @Transactional
    @Override
    public void updateUserStatus(Long id, Integer status) {
        if(id==null || status == null){
            throw new BusinessException("参数异常无法查询");
        }
        String currentRole = BaseContext.getCurrentRole();
        if (!RoleConstant.COMMON_ADMIN.equals(currentRole) && !RoleConstant.SUPER_ADMIN.equals(currentRole)) {
            throw new PermissionDeniedException("权限不足，无法查找");
        }

        //获取该用户所有商品ID
        List<Long> itemIds = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                        .select(Item::getId)
                        .eq(Item::getSellerId, id))
                .stream()
                .map(Item::getId)
                .collect(Collectors.toList());
        Integer targetItemStatus;
        if (status.equals(UserStatusConstant.NORMAL)) {
            // 【解封】：将之前被锁定的商品恢复为上架（或者下架，看业务需求，通常恢复为 LOCKED 之前的状态）
            // 这里简单处理：将被锁定的(2)恢复为上架(1)
            targetItemStatus = ItemStatusConstant.ON_SALE;
            itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                    .set(Item::getStatus, targetItemStatus)
                    .set(Item::getUpdateTime, LocalDateTime.now())
                    .eq(Item::getSellerId, id)
                    .eq(Item::getStatus, ItemStatusConstant.LOCKED)); // 只恢复那些因为封号被锁定的
        } else {
            //封禁
            targetItemStatus = ItemStatusConstant.LOCKED;

            itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                    .set(Item::getStatus, targetItemStatus)
                    .set(Item::getUpdateTime, LocalDateTime.now())
                    .eq(Item::getSellerId, id)
                    .in(Item::getStatus, List.of(ItemStatusConstant.ON_SALE, ItemStatusConstant.AUDIT_PENDING)));
        }
        if (!itemIds.isEmpty()) {
            List<String> keys = itemIds.stream()
                    .map(itemId -> RedisConstant.ITEM_DETAIL + itemId)
                    .collect(Collectors.toList());
            stringRedisTemplate.delete(keys);
            log.info("用户 {} 状态变更，清理了 {} 个商品的详情缓存", id, keys.size());
        }


        User user = new User();
        user.setId(id);
        user.setStatus(status);
        usersMapper.updateById(user);

        if (!itemIds.isEmpty()) {
            Integer targetStatus = status.equals(UserStatusConstant.NORMAL)
                    ? ItemStatusConstant.ON_SALE : ItemStatusConstant.LOCKED;

            itemEsSender.sendBatchUpdateStatusMessage(itemIds, targetStatus);
        }
        clearCache(itemIds);
    }

    private void clearCache(List<Long> itemIds) {
        List<String> keys = itemIds.stream()
                .map(itemId -> RedisConstant.ITEM_DETAIL + itemId)
                .collect(Collectors.toList());
        stringRedisTemplate.delete(keys);
    }
}