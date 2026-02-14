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

    Account findAccountByUsernameOrEmail(String text);

    String registerEmailVerifyCode(String type, String email, String ip);

    String registerEmailAccount(EmailRegisterRO ro);

    String resetConfirm(ConfirmResetRO ro);

    String resetEmailAccountPassword(EmailResetRO ro);

    String createAccount(CreateAccountRO ro);

    String deleteAccount(Long id, Long currentAdminId);

    String updateAccount(UpdateAccountRO ro, Long userId);

    IPage<AccountVO> listAccounts(ListAccountRO ro);

    AccountVO getAccountInfo(Long userId);

    /**
     * 更新用户头像 URL（仅更新 avatar 字段）
     *
     * @param userId    用户 ID
     * @param avatarUrl 头像完整 URL
     * @return null 表示成功，否则为错误信息
     */
    String updateAvatar(Long userId, String avatarUrl);
}
