package ynu.jackielinn.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.security.core.userdetails.UserDetailsService;
import ynu.jackielinn.server.dto.request.ConfirmResetRO;
import ynu.jackielinn.server.dto.request.CreateAccountRO;
import ynu.jackielinn.server.dto.request.EmailRegisterRO;
import ynu.jackielinn.server.dto.request.EmailResetRO;
import ynu.jackielinn.server.dto.request.ListAccountRO;
import ynu.jackielinn.server.dto.request.UpdateAccountRO;
import ynu.jackielinn.server.dto.response.AccountVO;
import ynu.jackielinn.server.entity.Account;

public interface AccountService extends IService<Account>, UserDetailsService {

    /**
     * 根据用户名或邮箱查询账户（用于登录时加载用户）。
     *
     * @param text 用户名或邮箱
     * @return 匹配的 Account，不存在返回 null
     */
    Account findAccountByUsernameOrEmail(String text);

    /**
     * 发送注册或重置密码的邮件验证码，并做限流与存储校验。
     *
     * @param type  register 或 reset
     * @param email 收件邮箱
     * @param ip    请求 IP（限流用）
     * @return null 表示成功，否则为错误信息
     */
    String registerEmailVerifyCode(String type, String email, String ip);

    /**
     * 邮箱注册：校验验证码后创建账户。
     *
     * @param ro 注册请求（电话、邮箱、验证码、用户名、密码）
     * @return null 表示成功，否则为错误信息
     */
    String registerEmailAccount(EmailRegisterRO ro);

    /**
     * 密码重置确认：校验邮箱与验证码是否有效。
     *
     * @param ro 确认请求（邮箱、验证码）
     * @return null 表示成功，否则为错误信息
     */
    String resetConfirm(ConfirmResetRO ro);

    /**
     * 邮箱重置密码：校验验证码后更新密码。
     *
     * @param ro 重置请求（邮箱、验证码、新密码）
     * @return null 表示成功，否则为错误信息
     */
    String resetEmailAccountPassword(EmailResetRO ro);

    /**
     * 管理员导入用户（默认密码 123456、角色 user）。
     *
     * @param ro 创建请求（用户名、邮箱、电话）
     * @return null 表示成功，否则为错误信息
     */
    String createAccount(CreateAccountRO ro);

    /**
     * 管理员逻辑删除用户（不能删除自己）。
     *
     * @param id             要删除的用户 id
     * @param currentAdminId 当前管理员 id
     * @return null 表示成功，否则为错误信息
     */
    String deleteAccount(Long id, Long currentAdminId);

    /**
     * 用户更新自己的信息（仅更新 ro 中非空字段）。
     *
     * @param ro     更新请求（用户名、性别、电话、生日等）
     * @param userId 当前用户 id（只能更新自己）
     * @return null 表示成功，否则为错误信息
     */
    String updateAccount(UpdateAccountRO ro, Long userId);

    /**
     * 管理员分页查询用户列表，支持关键字（用户名/邮箱/电话）与时间范围。
     *
     * @param ro 查询条件（关键字、分页、时间范围）
     * @return 分页结果（AccountVO，排除敏感信息）
     */
    IPage<AccountVO> listAccounts(ListAccountRO ro);

    /**
     * 查询当前用户自己的信息（AccountVO，排除密码等敏感字段）。
     *
     * @param userId 当前用户 id
     * @return 用户信息 VO，不存在返回 null
     */
    AccountVO getAccountInfo(Long userId);

    /**
     * 更新用户头像 URL（仅更新 avatar 字段）。
     *
     * @param userId    用户 id
     * @param avatarUrl 头像完整 URL
     * @return null 表示成功，否则为错误信息
     */
    String updateAvatar(Long userId, String avatarUrl);
}
