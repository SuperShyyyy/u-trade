package com.u.user.service.impl;

import com.u.api.client.wallet.WalletClient;
import com.u.common.constant.JwtClaimsConstant;
import com.u.common.constant.RedisConstant;
import com.u.common.context.BaseContext;
import com.u.user.domain.dto.ChangePasswordDTO;
import com.u.user.domain.dto.LoginDTO;
import com.u.user.domain.dto.RegisterDTO;
import com.u.user.domain.dto.UserDTO;
import com.u.user.domain.po.User;
import com.u.user.domain.vo.LoginVO;
import com.u.user.domain.vo.UserVO;
import com.u.common.exception.BusinessException;
import com.u.user.mapper.UserMapper;
import com.u.common.properties.JwtProperties;
import com.u.user.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.u.common.utils.JwtUtil;
import com.u.common.result.Result;
import com.u.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.alibaba.fastjson2.JSON;

import java.util.concurrent.TimeUnit;
/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    private final JwtProperties jwtProperties;
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final StringRedisTemplate stringRedisTemplate;
    private final WalletClient walletClient;

    private static final String LOGIN_FAIL_PREFIX = "user:login:fail:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    @Override
    public LoginVO userLogin(LoginDTO loginDTO) {
        // 登录限流：检查是否被临时锁定
        String failKey = LOGIN_FAIL_PREFIX + loginDTO.getUsername();
        String failCountStr = stringRedisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr != null ? Integer.parseInt(failCountStr) : 0;
        if (failCount >= MAX_LOGIN_ATTEMPTS) {
            throw new BusinessException("登录失败次数过多，账号已被临时锁定，请" + LOCK_DURATION_MINUTES + "分钟后重试");
        }

        // 1. 根据账号查询用户
        User user = lambdaQuery().eq(User::getUsername, loginDTO.getUsername()).one();
        // 2. 校验用户是否存在
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if(user.getStatus() == 0){
            throw new BusinessException("用户已被封禁");
        }
        // 3. 校验密码 BCrypt比对

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            // 登录失败，增加失败计数
            stringRedisTemplate.opsForValue().set(failKey, String.valueOf(failCount + 1),
                    LOCK_DURATION_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
            throw new BusinessException("密码错误");
        }

        // 登录成功，清除失败计数
        stringRedisTemplate.delete(failKey);

        // 4. 获取/初始化 tokenVersion（首次登录为 0）
        String versionKey = RedisConstant.TOKEN_VERSION_KEY + user.getId();
        String v = stringRedisTemplate.opsForValue().get(versionKey);
        int tokenVersion = (v != null) ? Integer.parseInt(v) : 0;
        if (v == null) {
            // 首次登录初始化版本号为 0，TTL 与 Token 有效期一致
            stringRedisTemplate.opsForValue().set(versionKey, "0",
                    jwtProperties.getUserTtl(), TimeUnit.MILLISECONDS);
        }

        // 5. 生成 sessionId（UUID），覆盖旧 session 实现单设备互踢
        String sessionId = UUID.randomUUID().toString();
        String sessionKey = RedisConstant.USER_SESSION_KEY + user.getId();
        stringRedisTemplate.opsForValue().set(sessionKey, sessionId,
                jwtProperties.getUserTtl(), TimeUnit.MILLISECONDS);

        // 6. 构造 JWT Claims
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.CURRENT_ID, user.getId());
        claims.put(JwtClaimsConstant.ROLE, null);
        claims.put(JwtClaimsConstant.SOURCE_TYPE, "USER");
        claims.put(JwtClaimsConstant.SESSION_ID, sessionId);
        claims.put(JwtClaimsConstant.TOKEN_VERSION, tokenVersion);

        // 7. 生成 Token
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims
        );

        log.info("用户登录成功, userId={}, sessionId={}, tokenVersion={}",
                user.getId(), sessionId, tokenVersion);

        // 8. 返回 VO
        return LoginVO.builder()
                .id(user.getId())
                .token(token)
                .role(null)
                .sourceType("USER")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        // 1 账号与手机号唯一性检查
        checkUnique(registerDTO);
        // 2 保存用户信息
        User user = new User();
        BeanUtils.copyProperties(registerDTO, user); // 修正：传入对象实例
        // DTO 获取原始密码
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        // 保存用户
        this.save(user);
        Long userId = user.getId();

        try {
            Result<Void> walletResult = walletClient.createWallet(userId);
            if (walletResult == null || !ResultCode.SUCCESS.equals(walletResult.getCode())) {
                throw new BusinessException("钱包创建失败，请重试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建钱包异常, userId={}", userId, e);
            throw new BusinessException("钱包服务不可用，请稍后重试");
        }
    }

    // 抽取检查逻辑
    private void checkUnique(RegisterDTO dto) {
        if (lambdaQuery().eq(User::getUsername, dto.getUsername()).count() > 0) {
            throw new BusinessException("用户名已被占用");
        }
        if (dto.getPhone() != null && lambdaQuery().eq(User::getPhone, dto.getPhone()).count() > 0) {
            throw new BusinessException("手机号已被注册");
        }
    }

    //查询个人信息
    @Override
    public UserVO queryMyInformation(){
        Long userId = BaseContext.getCurrentId();
        String cacheKey = RedisConstant.USER_INFO_KEY + userId;

        // 1. 尝试从缓存读取
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        if (json != null) {
            return JSON.parseObject(json, UserVO.class);
        }

        // 2. 缓存没有，从数据库读取
        User user = lambdaQuery().eq(User::getId, userId).one();
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(user == null ? "用户为空" : "用户已被封禁");
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        // 3. 写回缓存
        stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(userVO), 30, TimeUnit.MINUTES);

        return userVO;
    }

    @Override
    public void updateUserInfo(UserDTO userDTO){
        Long userId = BaseContext.getCurrentId();
        // 乐观锁：先查获取 version，再条件更新（lambdaUpdate 不触发 @Version 自动乐观锁，需手动处理）
        User currentUser = getById(userId);
        if (currentUser == null) {
            throw new BusinessException("用户不存在");
        }
        lambdaUpdate()
                .eq(User::getId, userId)
                .eq(User::getVersion, currentUser.getVersion())
                .set(User::getUsername, userDTO.getUsername())
                .set(User::getPhone, userDTO.getPhone())
                .set(User::getAvatar, userDTO.getAvatar())
                .setSql("version = version + 1")
                .update();
        stringRedisTemplate.delete(RedisConstant.USER_INFO_KEY + userId);
    }

    @Override
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        Long userId = BaseContext.getCurrentId();
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 校验原密码
        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (passwordEncoder.matches(changePasswordDTO.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        // 更新密码
        lambdaUpdate()
                .eq(User::getId, userId)
                .eq(User::getVersion, user.getVersion())
                .set(User::getPassword, passwordEncoder.encode(changePasswordDTO.getNewPassword()))
                .setSql("version = version + 1")
                .update();
        stringRedisTemplate.delete(RedisConstant.USER_INFO_KEY + userId);
        // tokenVersion +1 -> 所有设备所有 Token 立即失效，强制重新登录
        incrTokenVersion(userId);
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 32) {
            throw new BusinessException("密码长度需在6-32位之间");
        }
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        lambdaUpdate()
                .eq(User::getId, userId)
                .eq(User::getVersion, user.getVersion())
                .set(User::getPassword, passwordEncoder.encode(newPassword))
                .setSql("version = version + 1")
                .update();
        stringRedisTemplate.delete(RedisConstant.USER_INFO_KEY + userId);
        // 管理员重置密码 -> 同样 tokenVersion +1 使所有 Token 失效
        incrTokenVersion(userId);
    }

    @Override
    public void logout(Long userId) {
        // tokenVersion +1 -> 当前 session 及所有设备 Token 失效
        incrTokenVersion(userId);
        // 清除 session，新登录必须重新生成
        stringRedisTemplate.delete(RedisConstant.USER_SESSION_KEY + userId);
    }

    /**
     * tokenVersion 递增：使所有已签发的旧版本 Token 全部失效
     * <p>
     * 原理：JWT 签发时携带 version=N；Redis 存储当前有效 version。
     * Gateway 校验: jwt.tokenVersion != redis.tokenVersion → 401。
     * INCR 确保原子递增，TTL 与 Token 有效期一致（过期自动清理）。
     */
    private void incrTokenVersion(Long userId) {
        String key = RedisConstant.TOKEN_VERSION_KEY + userId;
        long ttl = jwtProperties.getUserTtl();
        try {
            Long newVersion = stringRedisTemplate.opsForValue().increment(key);
            if (newVersion != null) {
                stringRedisTemplate.expire(key, ttl, TimeUnit.MILLISECONDS);
            }
            log.info("tokenVersion 递增成功, userId={}, newVersion={}", userId, newVersion);
        } catch (Exception e) {
            log.error("tokenVersion 递增失败 (Redis异常), userId={}", userId, e);
        }
    }
}
