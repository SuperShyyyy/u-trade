package com.u.common.constant;

/**
 * JWT Claims 常量 (优化版)
 * 设计策略：
 * 1. 统一 ID 字段：currentId (屏蔽 user/admin 表差异)
 * 2. 角色驱动权限：role 决定能做什么
 * 3. 来源辅助定位：sourceType 决定查哪张表 (可选，但推荐)
 */
public class JwtClaimsConstant {

    /**
     * 统一的主键 ID
     * 无论是 user 表还是 admin 表，登录成功后都存入此字段
     */
    public static final String CURRENT_ID = "currentId";

    /**
     * 角色标识
     * 值约定：
     * - null (或空字符串) : C端用户 (买家/卖家/学生/求职者)
     * - "1" : B端普通管理员
     * - "2" : B端超级管理员
     */
    public static final String ROLE = "role";

    /**
     * 来源类型标识 (辅助字段，方便Service层判断查哪张表)
     * - "USER" : 来自 user 表
     * - "ADMIN" : 来自 admin 表
     */
    public static final String SOURCE_TYPE = "sourceType";
}
