package com.u.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class SerialNoUtil {

    private static final Random RANDOM = new Random();

    // 时间格式化器
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private SerialNoUtil() {} // 私有构造，防止实例化

    /**
     * 生成订单号
     * @return 订单号，格式：ORD + yyyyMMddHHmmssSSS + 4位随机数
     */
    public static String generateOrderNo() {
        return "ORD" + now() + random6();
    }
    /**
     * 生成支付号
     * @return 支付号，格式：PAY + yyyyMMddHHmmssSSS + 4位随机数
     */
    public static String generatePayNo() {
        return "PAY" + now() + random6();
    }
    /**
     * 生成物流号
     * @return 物流号，格式：SHIP + yyyyMMddHHmmssSSS + 4位随机数
     */
    public static String generateShipmentNo() {
        return "SHIP" + now() + random6();
    }

    /**
     * 获取当前时间戳字符串
     */
    private static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }


    public static String generateRechargeNo() {return "RECHARGE"+ now() + random6();}


    public static String generateWithdrawNo() {return "WITHDRAW"+ now() + random6();}

    /**
     * 生成6位随机数，前面补0
     */
    private static String random6() {
        int num = RANDOM.nextInt(1000000); // 0~9999
        return String.format("%06d", num);
    }



}
