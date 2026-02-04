package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.dto.request.ConfirmResetRO;
import ynu.jackielinn.server.dto.request.EmailRegisterRO;
import ynu.jackielinn.server.dto.request.EmailResetRO;
import ynu.jackielinn.server.entity.Account;
import ynu.jackielinn.server.common.Gender;
import ynu.jackielinn.server.mapper.AccountMapper;
import ynu.jackielinn.server.service.AccountService;
import ynu.jackielinn.server.utils.Const;
import ynu.jackielinn.server.utils.FlowUtils;

import java.time.LocalDate;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    @Resource
    FlowUtils flowUtils;

    @Resource
    AmqpTemplate amqpTemplate;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    PasswordEncoder encoder;

    /**
     * 从数据库中通过用户名或邮箱查找用户详细信息
     *
     * @param username 用户名
     * @return 用户详细信息
     * @throws UsernameNotFoundException 如果用户未找到则抛出此异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = this.findAccountByUsernameOrEmail(username);
        if (account == null)
            throw new UsernameNotFoundException("用户名或密码错误");
        return User
                .withUsername(username)
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }

    /**
     * 根据用户名、邮箱或电话号码查找账户
     *
     * @param text 用户名、邮箱或电话号码
     * @return 找到的账户，如果没有找到则返回 null
     */
    @Override
    public Account findAccountByUsernameOrEmail(String text) {
        return this.query()
                .eq("username", text).or()
                .eq("email", text).or()
                .eq("telephone", text)
                .one();
    }

    /**
     * 注册或重置密码时，生成并发送电子邮件验证码
     *
     * @param type  操作类型，"register" 表示注册，"reset" 表示重置密码
     * @param email 要发送验证码的电子邮件地址
     * @param ip    发送验证码请求的 IP 地址，用于限制请求频率
     * @return 如果操作成功，返回 null；如果请求过于频繁，返回错误信息
     */
    @Override
    public String registerEmailVerifyCode(String type, String email, String ip) {
        synchronized (ip.intern()) {
            if (!this.verifyLimit(ip)) {
                return "请求频繁，请稍后再试";
            }
            Random random = new Random();
            int code = random.nextInt(899999) + 100000;
            Map<String, Object> data = Map.of("type", type, "email", email, "code", code);
            amqpTemplate.convertAndSend("MHFLVSMail", data);
            stringRedisTemplate.opsForValue()
                    .set(Const.VERIFY_EMAIL_DATA + email, String.valueOf(code), 3, TimeUnit.MINUTES);
            return null;
        }
    }

    /**
     * 邮件验证码注册账号操作，需要检查验证码是否正确以及邮箱、用户名是否存在重名
     *
     * @param ro 注册基本信息
     * @return 操作结果，null表示正常，否则为错误原因
     */
    @Override
    public String registerEmailAccount(EmailRegisterRO ro) {
        String email = ro.getEmail();
        String telephone = ro.getTelephone();
        String username = ro.getUsername();
        String key = Const.VERIFY_EMAIL_DATA + email;
        String code = stringRedisTemplate.opsForValue().get(key);
        if (code == null) return "请先获取验证码";
        if (!code.equals(ro.getCode())) return "验证码错误，请重新输入";
        if (this.existsAccountByEmail(email)) return "此电子邮件已被其他用户注册";
        if (this.existsAccountByUsername(username)) return "此用户名已被其他人注册，请更换一个新的用户名";
        if (this.existsAccountByTelephone(telephone)) return "此电话号码已被其他人注册";
        String password = encoder.encode(ro.getPassword());
        Account account = Account.builder()
                .username(username)
                .password(password)
                .gender(Gender.UNKNOWN)
                .email(email)
                .telephone(telephone)
                .avatar("https://avatars.githubusercontent.com/u/136216354?s=96&v=4")
                .role("user")
                .build();
        if (this.save(account)) {
            stringRedisTemplate.delete(key);
            return null;
        } else {
            return "内部错误，请联系管理员";
        }
    }

    /**
     * 重置密码确认操作，验证验证码是否正确
     *
     * @param ro 验证基本信息
     * @return 操作结果，null表示正常，否则为错误原因
     */
    @Override
    public String resetConfirm(ConfirmResetRO ro) {
        String email = ro.getEmail();
        String code = stringRedisTemplate.opsForValue().get(Const.VERIFY_EMAIL_DATA + email);
        if (code == null) return "请先获取验证码";
        if (!code.equals(ro.getCode())) return "验证码错误，请重新输入";
        return null;
    }

    /**
     * 邮件验证码重置密码操作，需要检查验证码是否正确
     *
     * @param ro 重置基本信息
     * @return 操作结果，null表示正常，否则为错误原因
     */
    @Override
    public String resetEmailAccountPassword(EmailResetRO ro) {
        String verify = resetConfirm(new ConfirmResetRO(ro.getEmail(), ro.getCode()));
        if (verify != null) return verify;
        String email = ro.getEmail();
        String password = encoder.encode(ro.getPassword());
        boolean update = this.update().eq("email", email).set("password", password).update();
        if (update) {
            stringRedisTemplate.delete(Const.VERIFY_EMAIL_DATA + email);
        }
        return update ? null : "更新失败，请联系管理员";
    }

    /**
     * 验证指定 IP 在一定时间内是否已经达到了发送邮件的次数限制
     *
     * @param ip 要验证的 IP 地址
     * @return 如果未达到限制，返回 true；如果已经达到限制，返回 false
     */
    private boolean verifyLimit(String ip) {
        String key = Const.VERIFY_EMAIL_LIMIT + ip;
        return flowUtils.limitOnceCheck(key, 60);
    }

    /**
     * 查询指定邮箱的用户是否已经存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    private boolean existsAccountByEmail(String email) {
        return this.baseMapper.exists(Wrappers.<Account>query().eq("email", email));
    }

    /**
     * 查询指定用户名的用户是否已经存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    private boolean existsAccountByUsername(String username) {
        return this.baseMapper.exists(Wrappers.<Account>query().eq("username", username));
    }

    /**
     * 查询指定用户名的用户是否已经存在
     *
     * @param telephone 用户名
     * @return 是否存在
     */
    private boolean existsAccountByTelephone(String telephone) {
        return this.baseMapper.exists(Wrappers.<Account>query().eq("telephone", telephone));
    }

    /**
     * 初始化默认管理员账号和测试用户
     * 在应用启动时检查 account 表是否有数据，没有则创建默认管理员和测试用户
     */
    @PostConstruct
    public void initDefaultAdmin() {
        long count = this.count();
        if (count == 0) {
            log.info("Account table is empty, creating default admin and test users...");
            String password = encoder.encode("123456");
            String defaultAvatar = "https://avatars.githubusercontent.com/u/136216354?s=96&v=4";
            LocalDate defaultBirthday = LocalDate.of(2000, 1, 1);

            // 创建管理员账号
            Account admin = Account.builder()
                    .username("admin")
                    .password(password)
                    .gender(Gender.MALE)
                    .email("123456@example.com")
                    .telephone("13888888888")
                    .avatar(defaultAvatar)
                    .role("admin")
                    .birthday(defaultBirthday)
                    .build();
            if (this.save(admin)) {
                log.info("Default admin account created successfully, username: admin, password: 123456");
            } else {
                log.error("Failed to create default admin account");
            }

            String[] testUsernames = {"test1", "test2", "test3"};
            String[] testEmails = {"234567@example.com", "345678@example.com", "456789@example.com"};
            String[] testPhones = {"13111111111", "13222222222", "13333333333"};

            // 创建测试用户
            for (int i = 0; i < testUsernames.length; i++) {
                Account testUser = Account.builder()
                        .username(testUsernames[i])
                        .password(password)
                        .gender(Gender.MALE)
                        .email(testEmails[i])
                        .telephone(testPhones[i])
                        .avatar(defaultAvatar)
                        .role("user")
                        .birthday(defaultBirthday)
                        .build();
                if (this.save(testUser)) {
                    log.info("Test user {} created successfully", testUsernames[i]);
                } else {
                    log.error("Failed to create test user {}", testUsernames[i]);
                }
            }
        } else {
            log.info("Account table already has {} records, skipping initialization", count);
        }
    }
}
