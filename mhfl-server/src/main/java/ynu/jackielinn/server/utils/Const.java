package ynu.jackielinn.server.utils;

/**
 * Const用来存储经常使用到的属性
 * 在此统一改比较方便
 */
public class Const {
    public static final String JWT_BLACK_LIST = "jwt:blacklist:";

    public static final int ORDER_CORS = -102;
    public static final int ORDER_LIMIT = -101;

    //邮件验证码
    public final static String VERIFY_EMAIL_LIMIT = "verify:email:limit:";
    public final static String VERIFY_EMAIL_DATA = "verify:email:data:";

    //请求频率限制
    public final static String FLOW_LIMIT_COUNTER = "flow:counter:";
    public final static String FLOW_LIMIT_BLOCK = "flow:block:";

    //图形验证码
    public final static String VERIFY_CAPTCHA_LIMIT = "verify:captcha:limit:";
    public final static String VERIFY_CAPTCHA_DATA = "verify:captcha:data:";
    public final static String VERIFY_CAPTCHA_FAIL_COUNT = "verify:captcha:fail:";

    // 联邦学习实验
    public final static String TASK_EXPERIMENT_ROUND = "task:experiment:round:";
    public final static String TASK_EXPERIMENT_CLIENT = "task:experiment:client:";
}
