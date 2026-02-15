package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import ynu.jackielinn.server.dto.request.CreateAccountRO;
import ynu.jackielinn.server.dto.request.EmailRegisterRO;
import ynu.jackielinn.server.dto.request.EmailResetRO;
import ynu.jackielinn.server.dto.request.ListAccountRO;
import ynu.jackielinn.server.dto.request.UpdateAccountRO;
import ynu.jackielinn.server.dto.response.AccountVO;
import ynu.jackielinn.server.entity.Account;
import ynu.jackielinn.server.common.Gender;
import ynu.jackielinn.server.mapper.AccountMapper;
import ynu.jackielinn.server.service.AccountService;
import ynu.jackielinn.server.utils.Const;
import ynu.jackielinn.server.utils.FlowUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
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
                .avatar(null)
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
     * 查询指定用户名是否已被其他用户（排除指定 id）使用
     *
     * @param username  用户名
     * @param excludeId 要排除的用户 id
     * @return 是否存在
     */
    private boolean existsAccountByUsernameExcludeId(String username, Long excludeId) {
        return this.baseMapper.exists(Wrappers.<Account>query()
                .eq("username", username)
                .ne("id", excludeId));
    }

    /**
     * 查询指定电话号码是否已被其他用户（排除指定 id）使用
     *
     * @param telephone 电话号码
     * @param excludeId 要排除的用户 id
     * @return 是否存在
     */
    private boolean existsAccountByTelephoneExcludeId(String telephone, Long excludeId) {
        return this.baseMapper.exists(Wrappers.<Account>query()
                .eq("telephone", telephone)
                .ne("id", excludeId));
    }

    /**
     * 管理员导入用户操作，需要检查用户名、邮箱、电话号码是否存在重名
     *
     * @param ro 用户基本信息
     * @return 操作结果，null表示正常，否则为错误原因
     */
    @Override
    public String createAccount(CreateAccountRO ro) {
        String username = ro.getUsername();
        String email = ro.getEmail();
        String telephone = ro.getTelephone();
        if (this.existsAccountByUsername(username)) return "此用户名已被其他人注册，请更换一个新的用户名";
        if (this.existsAccountByEmail(email)) return "此电子邮件已被其他用户注册";
        if (this.existsAccountByTelephone(telephone)) return "此电话号码已被其他人注册";
        String password = encoder.encode("123456");
        Account account = Account.builder()
                .username(username)
                .password(password)
                .gender(Gender.UNKNOWN)
                .email(email)
                .telephone(telephone)
                .avatar(null)
                .role("user")
                .birthday(null)
                .build();
        if (this.save(account)) {
            return null;
        } else {
            return "内部错误，请联系管理员";
        }
    }

    /**
     * 管理员逻辑删除用户（手动设置 is_deleted = 1 和 delete_time）
     * 使用 updateById 会触发 updateFill，自动更新 update_time 为删除时间
     *
     * @param id             要删除的用户 id
     * @param currentAdminId 当前管理员 id（从 JWT token 中获取）
     * @return null 表示成功，否则为错误信息
     */
    @Override
    public String deleteAccount(Long id, Long currentAdminId) {
        Account account = this.getById(id);
        if (account == null) {
            return "用户不存在";
        }
        // 防止管理员删除自己
        if (id.equals(currentAdminId)) {
            return "不能删除自己的账户";
        }
        // 手动设置删除时间和删除标记，使用 updateById 会触发 updateFill，自动更新 update_time
        Account deleteAccount = Account.builder()
                .id(id)
                .deleted(1)
                .deleteTime(LocalDateTime.now())
                .build();
        if (this.updateById(deleteAccount)) {
            return null;
        }
        return "删除失败，请联系管理员";
    }

    /**
     * 更新用户信息（username, gender, telephone, birthday）
     * 只更新传入的非 null 字段，username 和 telephone 需要检查唯一性（排除当前用户）
     * 用户只能更新自己的信息，管理员也不能修改其他用户的信息
     *
     * @param ro     更新信息对象
     * @param userId 要更新的用户 id（当前登录用户的 id，从 JWT token 中获取）
     * @return null 表示成功，否则为错误信息
     */
    @Override
    public String updateAccount(UpdateAccountRO ro, Long userId) {
        Account account = this.getById(userId);
        if (account == null) {
            return "用户不存在";
        }

        // 检查 username 唯一性（如果提供了 username）
        if (ro.getUsername() != null && !ro.getUsername().isEmpty()) {
            if (existsAccountByUsernameExcludeId(ro.getUsername(), userId)) {
                return "此用户名已被其他用户注册，请更换一个新的用户名";
            }
        }

        // 检查 telephone 唯一性（如果提供了 telephone）
        if (ro.getTelephone() != null && !ro.getTelephone().isEmpty()) {
            if (existsAccountByTelephoneExcludeId(ro.getTelephone(), userId)) {
                return "此电话号码已被其他用户注册";
            }
        }

        // 构建更新对象，只设置非 null 字段
        Account updateAccount = Account.builder()
                .id(userId)
                .build();

        if (ro.getUsername() != null) {
            updateAccount.setUsername(ro.getUsername());
        }
        if (ro.getGender() != null) {
            updateAccount.setGender(ro.getGender());
        }
        if (ro.getTelephone() != null) {
            updateAccount.setTelephone(ro.getTelephone());
        }
        if (ro.getBirthday() != null) {
            updateAccount.setBirthday(ro.getBirthday());
        }

        // 使用 updateById 更新，MyBatis-Plus 会自动只更新非 null 字段
        if (this.updateById(updateAccount)) {
            return null;
        } else {
            return "更新失败，请联系管理员";
        }
    }

    /**
     * 管理员查询用户列表（支持关键字模糊查询和时间范围查询，分页）
     * 关键字为空则查询全部，不为空则在用户名、邮箱、电话号码中模糊查询（叠加查询，OR 连接）
     *
     * @param ro 查询条件对象
     * @return 分页结果（AccountVO，排除敏感信息）
     */
    @Override
    public IPage<AccountVO> listAccounts(ListAccountRO ro) {
        // 处理默认值（@ModelAttribute 绑定可能不会应用 @Builder.Default）
        long current = ro.getCurrent() != null ? ro.getCurrent() : 1L;
        long size = ro.getSize() != null ? ro.getSize() : 10L;
        Page<Account> page = new Page<>(current, size);
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();

        // 关键字模糊查询（叠加查询）
        if (ro.getKeyword() != null && !ro.getKeyword().trim().isEmpty()) {
            String keywordTrimmed = ro.getKeyword().trim();
            // 叠加查询：在用户名、邮箱、电话号码中模糊查询，使用 OR 连接
            wrapper.and(w -> w
                    .like(Account::getUsername, keywordTrimmed)
                    .or()
                    .like(Account::getEmail, keywordTrimmed)
                    .or()
                    .like(Account::getTelephone, keywordTrimmed)
            );
        }

        // 创建时间范围查询（将 LocalDate 转换为 LocalDateTime）
        LocalDate startDate = ro.getStartTime();
        LocalDate endDate = ro.getEndTime();
        if (startDate != null && endDate != null) {
            // 两个时间都有：时间范围内（起始时间当天 00:00:00 到终止时间当天 23:59:59）
            LocalDateTime startTime = startDate.atStartOfDay();
            LocalDateTime endTime = endDate.atTime(23, 59, 59);
            wrapper.between(Account::getCreateTime, startTime, endTime);
        } else if (startDate != null) {
            // 起始时间有，终止时间空：从起始时间当天 00:00:00 到现在
            LocalDateTime startTime = startDate.atStartOfDay();
            wrapper.ge(Account::getCreateTime, startTime);
        } else if (endDate != null) {
            // 起始时间空，终止时间有：终止时间当天 23:59:59 之前
            LocalDateTime endTime = endDate.atTime(23, 59, 59);
            wrapper.le(Account::getCreateTime, endTime);
        }
        // 两个时间都空：不限时间（不添加条件）

        // 按 id 从小到大排序
        wrapper.orderByAsc(Account::getId);

        // 查询Account分页结果
        IPage<Account> accountPage = this.page(page, wrapper);

        // 转换为AccountVO分页结果（年龄用 asViewObject 的 consumer 填充）
        Page<AccountVO> voPage = new Page<>(accountPage.getCurrent(), accountPage.getSize(), accountPage.getTotal());
        voPage.setRecords(accountPage.getRecords().stream()
                .map(account -> account.asViewObject(AccountVO.class, vo -> vo.setAge(
                        vo.getBirthday() == null ? null : Period.between(vo.getBirthday(), LocalDate.now()).getYears())))
                .toList());

        return voPage;
    }

    /**
     * 用户查询自己的信息（只能查询自己的信息）
     *
     * @param userId 用户ID（当前登录用户的ID，从JWT token中获取）
     * @return 用户信息（AccountVO，排除敏感信息）
     */
    @Override
    public AccountVO getAccountInfo(Long userId) {
        Account account = this.getById(userId);
        if (account == null) {
            return null;
        }
        return account.asViewObject(AccountVO.class, vo -> vo.setAge(
                vo.getBirthday() == null ? null : Period.between(vo.getBirthday(), LocalDate.now()).getYears()));
    }

    @Override
    public String updateAvatar(Long userId, String avatarUrl) {
        Account account = this.getById(userId);
        if (account == null) {
            return "用户不存在";
        }
        Account update = Account.builder().id(userId).avatar(avatarUrl).build();
        return this.updateById(update) ? null : "更新头像失败，请联系管理员";
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

            // 创建管理员账号
            Account admin = Account.builder()
                    .username("admin")
                    .password(password)
                    .gender(Gender.MALE)
                    .email("123456@example.com")
                    .telephone("13888888888")
                    .avatar(null)
                    .role("admin")
                    .birthday(null)
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
                        .avatar(null)
                        .role("user")
                        .birthday(null)
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
