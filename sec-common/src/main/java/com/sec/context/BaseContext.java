package com.sec.context;

public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();
    // 新增
    private static ThreadLocal<String> roleThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<String> sourceTypeThreadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    // 新增方法
    public static void setCurrentRole(String role) {
        roleThreadLocal.set(role);
    }
    public static String getCurrentRole() {
        return roleThreadLocal.get();
    }

    public static void setCurrentSourceType(String type) {
        sourceTypeThreadLocal.set(type);
    }
    public static String getCurrentSourceType() {
        return sourceTypeThreadLocal.get();
    }

    // 记得在移除上下文时 clear 所有 threadLocal
    public static void remove() {
        threadLocal.remove();
        roleThreadLocal.remove();
        sourceTypeThreadLocal.remove();
    }
}