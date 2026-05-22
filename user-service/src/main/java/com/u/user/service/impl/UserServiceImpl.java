package com.u.user.service.impl;

import com.u.common.constant.JwtClaimsConstant;
import com.u.common.constant.RedisConstant;
import com.u.common.context.BaseContext;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    private final JwtProperties jwtProperties;
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final StringRedisTemplate stringRedisTemplate;
    @Override
    public LoginVO userLogin(LoginDTO loginDTO) {
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
            throw new BusinessException("密码错误");
        }

        // 4. 构造 JWT Claims
        Map<String, Object> claims = new HashMap<>();

        // 统一 ID 字段
        claims.put(JwtClaimsConstant.CURRENT_ID, user.getId());

        //  角色设为 null 代表普通用户
        claims.put(JwtClaimsConstant.ROLE, null);

        // 来源标识
        claims.put(JwtClaimsConstant.SOURCE_TYPE, "USER");

        // 5. 生成 Token
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(), // 用户专属密钥
                jwtProperties.getUserTtl(),       // 用户 Token 有效期
                claims
        );

        // 6. 返回 VO
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
        lambdaUpdate()
                .eq(User::getId,userId)
                .set(User::getUsername, userDTO.getUsername())
                .set(User::getPhone, userDTO.getPhone())
                .set(User::getAvatar, userDTO.getAvatar())
                .update();
        stringRedisTemplate.delete(RedisConstant.USER_INFO_KEY + userId);
    }
}
