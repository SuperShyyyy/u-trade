package com.sec.service;

import com.sec.domain.dto.AdminDTO;
import com.sec.domain.dto.LoginDTO;
import com.sec.domain.po.Admin;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sec.domain.vo.LoginVO;
import com.sec.domain.vo.UserVO;
import com.sec.result.PageDTO;
import com.sec.result.Result;

/**
 * <p>
 * 系统管理员表 服务类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
public interface IAdminService extends IService<Admin> {

    LoginVO adminLogin(LoginDTO loginDTO);

    PageDTO pageQuery(int page, int pageSize);

    void saveAdmin(AdminDTO adminDTO);

    void updateAdminStatus(Long id, Integer status);

    void deleteAdminById(Long adminId);

    void updateUserStatus(Long id, Integer status);

    PageDTO<UserVO> pageQueryUser(int page, int pageSize);
}
