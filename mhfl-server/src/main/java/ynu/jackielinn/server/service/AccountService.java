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

    IPage<Account> listAccounts(ListAccountRO ro);
}
