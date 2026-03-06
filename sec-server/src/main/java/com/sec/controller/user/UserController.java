package com.sec.controller.user;


import com.sec.domain.dto.LoginDTO;
import com.sec.domain.dto.RegisterDTO;
import com.sec.domain.dto.UserDTO;
import com.sec.domain.po.User;
import com.sec.domain.vo.LoginVO;
import com.sec.domain.vo.UserPublicVO;
import com.sec.domain.vo.UserVO;
import com.sec.exception.BusinessException;
import com.sec.result.Result;
import com.sec.service.IUserService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@RestController
@RequestMapping("/user")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    /**
     * C 端用户登录
     * POST /user/login
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        log.info("用户登录：{}", loginDTO.getUsername());

        LoginVO loginVO = userService.userLogin(loginDTO);

        return Result.success(loginVO);
    }

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO registerDTO) {
        log.info("新用户注册请求：{}", registerDTO.getUsername());
        userService.register(registerDTO);
        return Result.success();
    }


    @ApiOperation("登陆后获取个人信息")
    @GetMapping("/me")
    public Result<UserVO> queryMyInformation(){
        UserVO userVO = userService.queryMyInformation();
        return Result.success(userVO);
    }

    @ApiOperation("修改用户信息")
    @PutMapping
    public Result<String> update(@RequestBody UserDTO userDTO) {
        log.info("修改用户信息：{}", userDTO);
        userService.updateUserInfo(userDTO);
        return Result.success("修改成功");
    }



    @ApiOperation("查询他人公开信息")
    @GetMapping("/{id}/public")
    public Result<UserPublicVO> getUserPublicInfo(@PathVariable Long id){
        User user = userService.getById(id);
        if(user == null){
            throw new BusinessException("用户不存在");
        }
        UserPublicVO vo = new UserPublicVO();
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setId(user.getId());
        vo.setCreditScore(user.getCreditScore());
        vo.setStatus(user.getStatus());
        return Result.success(vo);
    }
}