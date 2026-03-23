package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import ynu.jackielinn.server.common.Gender;
import ynu.jackielinn.server.dto.request.ConfirmResetRO;
import ynu.jackielinn.server.dto.request.CreateAccountRO;
import ynu.jackielinn.server.dto.request.EmailRegisterRO;
import ynu.jackielinn.server.dto.request.EmailResetRO;
import ynu.jackielinn.server.dto.request.ListAccountRO;
import ynu.jackielinn.server.dto.request.UpdateAccountRO;
import ynu.jackielinn.server.dto.response.AccountVO;
import ynu.jackielinn.server.entity.Account;
import ynu.jackielinn.server.mapper.AccountMapper;
import ynu.jackielinn.server.utils.Const;
import ynu.jackielinn.server.utils.FlowUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Spy
    @InjectMocks
    private AccountServiceImpl service;

    @Mock
    private FlowUtils flowUtils;

    @Mock
    private AmqpTemplate amqpTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", accountMapper);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void loadUserByUsernameShouldThrowWhenAccountMissing() {
        doReturn(null).when(service).findAccountByUsernameOrEmail("missing");

        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsernameShouldReturnUserDetailsWhenAccountExists() {
        Account account = Account.builder().username("u").password("pwd").role("admin").build();
        doReturn(account).when(service).findAccountByUsernameOrEmail("u");

        UserDetails details = service.loadUserByUsername("u");

        assertThat(details.getUsername()).isEqualTo("u");
        assertThat(details.getPassword()).isEqualTo("pwd");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_admin");
    }

    @Test
    void findAccountByUsernameOrEmailShouldReturnQueryResult() {
        QueryChainWrapper<Account> chain = mock(QueryChainWrapper.class);
        Account target = Account.builder().username("tom").build();
        doReturn(chain).when(service).query();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.or()).thenReturn(chain);
        when(chain.one()).thenReturn(target);

        Account account = service.findAccountByUsernameOrEmail("tom");

        assertThat(account).isSameAs(target);
    }

    @Test
    void registerEmailVerifyCodeShouldFailWhenLimited() {
        when(flowUtils.limitOnceCheck(eq(Const.VERIFY_EMAIL_LIMIT + "127.0.0.1"), eq(60))).thenReturn(false);

        String result = service.registerEmailVerifyCode("register", "a@b.com", "127.0.0.1");

        assertThat(result).isNotNull();
    }

    @Test
    void registerEmailVerifyCodeShouldSendCodeWhenAllowed() {
        when(flowUtils.limitOnceCheck(eq(Const.VERIFY_EMAIL_LIMIT + "127.0.0.1"), eq(60))).thenReturn(true);

        String result = service.registerEmailVerifyCode("register", "a@b.com", "127.0.0.1");

        assertThat(result).isNull();
        verify(amqpTemplate, times(1)).convertAndSend(eq("MHFLVSMail"), any(Map.class));
        verify(valueOperations, times(1)).set(eq(Const.VERIFY_EMAIL_DATA + "a@b.com"), anyString(), eq(3L), eq(TimeUnit.MINUTES));
    }

    @Test
    void registerEmailAccountShouldFailWhenCodeMissing() {
        EmailRegisterRO ro = EmailRegisterRO.builder().email("a@b.com").code("123456").build();
        when(valueOperations.get(Const.VERIFY_EMAIL_DATA + "a@b.com")).thenReturn(null);

        String result = service.registerEmailAccount(ro);

        assertThat(result).isNotNull();
    }

    @Test
    void registerEmailAccountShouldFailWhenCodeMismatch() {
        EmailRegisterRO ro = EmailRegisterRO.builder().email("a@b.com").code("123456").build();
        when(valueOperations.get(Const.VERIFY_EMAIL_DATA + "a@b.com")).thenReturn("654321");

        String result = service.registerEmailAccount(ro);

        assertThat(result).isNotNull();
    }

    @Test
    void registerEmailAccountShouldFailWhenEmailExists() {
        EmailRegisterRO ro = EmailRegisterRO.builder()
                .email("a@b.com")
                .code("123456")
                .username("u")
                .telephone("13800000000")
                .password("p")
                .build();
        when(valueOperations.get(Const.VERIFY_EMAIL_DATA + "a@b.com")).thenReturn("123456");
        when(accountMapper.exists(any(QueryWrapper.class))).thenReturn(true);

        String result = service.registerEmailAccount(ro);

        assertThat(result).isNotNull();
    }

    @Test
    void registerEmailAccountShouldFailWhenUsernameExists() {
        EmailRegisterRO ro = EmailRegisterRO.builder()
                .email("a@b.com")
                .code("123456")
                .username("u")
                .telephone("13800000000")
                .password("p")
                .build();
        when(valueOperations.get(Const.VERIFY_EMAIL_DATA + "a@b.com")).thenReturn("123456");
        when(accountMapper.exists(any(QueryWrapper.class))).thenReturn(false, true);

        String result = service.registerEmailAccount(ro);

        assertThat(result).isNotNull();
    }

    @Test
    void registerEmailAccountShouldFailWhenTelephoneExists() {
        EmailRegisterRO ro = EmailRegisterRO.builder()
                .email("a@b.com")
                .code("123456")
                .username("u")
                .telephone("13800000000")
                .password("p")
                .build();
        when(valueOperations.get(Const.VERIFY_EMAIL_DATA + "a@b.com")).thenReturn("123456");
        when(accountMapper.exists(any(QueryWrapper.class))).thenReturn(false, false, true);

        String result = service.registerEmailAccount(ro);

        assertThat(result).isNotNull();
    }

    @Test
    void registerEmailAccountShouldSucceedAndDeleteCode() {
        EmailRegisterRO ro = EmailRegisterRO.builder()
                .email("a@b.com")
                .code("123456")
                .username("u")
                .telephone("13800000000")
                .password("p")
                .build();
        when(valueOperations.get(Const.VERIFY_EMAIL_DATA + "a@b.com")).thenReturn("123456");
        when(accountMapper.exists(any(QueryWrapper.class))).thenReturn(false, false, false);
        when(encoder.encode("p")).thenReturn("enc");
        doReturn(true).when(service).save(any(Account.class));

        String result = service.registerEmailAccount(ro);

        assertThat(result).isNull();
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(service).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("user");
        assertThat(captor.getValue().getGender()).isEqualTo(Gender.UNKNOWN);
        verify(stringRedisTemplate).delete(Const.VERIFY_EMAIL_DATA + "a@b.com");
    }

    @Test
    void registerEmailAccountShouldFailWhenSaveFailed() {
        EmailRegisterRO ro = EmailRegisterRO.builder()
                .email("a@b.com")
                .code("123456")
                .username("u")
                .telephone("13800000000")
                .password("p")
                .build();
        when(valueOperations.get(Const.VERIFY_EMAIL_DATA + "a@b.com")).thenReturn("123456");
        when(accountMapper.exists(any(QueryWrapper.class))).thenReturn(false, false, false);
        when(encoder.encode("p")).thenReturn("enc");
        doReturn(false).when(service).save(any(Account.class));

        String result = service.registerEmailAccount(ro);

        assertThat(result).isNotNull();
    }

    @Test
    void resetConfirmShouldFailWhenCodeMissing() {
        ConfirmResetRO ro = ConfirmResetRO.builder().email("a@b.com").code("111111").build();
        when(valueOperations.get(Const.VERIFY_EMAIL_DATA + "a@b.com")).thenReturn(null);

        String result = service.resetConfirm(ro);

        assertThat(result).isNotNull();
    }

    @Test
    void resetConfirmShouldFailWhenCodeWrong() {
        ConfirmResetRO ro = ConfirmResetRO.builder().email("a@b.com").code("111111").build();
        when(valueOperations.get(Const.VERIFY_EMAIL_DATA + "a@b.com")).thenReturn("222222");

        String result = service.resetConfirm(ro);

        assertThat(result).isNotNull();
    }

    @Test
    void resetConfirmShouldSucceedWhenCodeMatches() {
        ConfirmResetRO ro = ConfirmResetRO.builder().email("a@b.com").code("111111").build();
        when(valueOperations.get(Const.VERIFY_EMAIL_DATA + "a@b.com")).thenReturn("111111");

        String result = service.resetConfirm(ro);

        assertThat(result).isNull();
    }

    @Test
    void resetEmailAccountPasswordShouldReturnVerifyError() {
        EmailResetRO ro = EmailResetRO.builder().email("a@b.com").code("111111").password("newp").build();
        doReturn("verify error").when(service).resetConfirm(any(ConfirmResetRO.class));

        String result = service.resetEmailAccountPassword(ro);

        assertThat(result).isEqualTo("verify error");
    }

    @Test
    void resetEmailAccountPasswordShouldSucceed() {
        EmailResetRO ro = EmailResetRO.builder().email("a@b.com").code("111111").password("newp").build();
        UpdateChainWrapper<Account> chain = mock(UpdateChainWrapper.class);
        doReturn(null).when(service).resetConfirm(any(ConfirmResetRO.class));
        when(encoder.encode("newp")).thenReturn("enc");
        doReturn(chain).when(service).update();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.set(anyString(), any())).thenReturn(chain);
        when(chain.update()).thenReturn(true);

        String result = service.resetEmailAccountPassword(ro);

        assertThat(result).isNull();
        verify(stringRedisTemplate).delete(Const.VERIFY_EMAIL_DATA + "a@b.com");
    }

    @Test
    void resetEmailAccountPasswordShouldFailWhenUpdateFailed() {
        EmailResetRO ro = EmailResetRO.builder().email("a@b.com").code("111111").password("newp").build();
        UpdateChainWrapper<Account> chain = mock(UpdateChainWrapper.class);
        doReturn(null).when(service).resetConfirm(any(ConfirmResetRO.class));
        when(encoder.encode("newp")).thenReturn("enc");
        doReturn(chain).when(service).update();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.set(anyString(), any())).thenReturn(chain);
        when(chain.update()).thenReturn(false);

        String result = service.resetEmailAccountPassword(ro);

        assertThat(result).isNotNull();
        verify(stringRedisTemplate, never()).delete(Const.VERIFY_EMAIL_DATA + "a@b.com");
    }

    @Test
    void createAccountShouldFailWhenUsernameExists() {
        CreateAccountRO ro = CreateAccountRO.builder().username("u").email("a@b.com").telephone("13800000000").build();
        when(accountMapper.exists(any(QueryWrapper.class))).thenReturn(true);

        String result = service.createAccount(ro);

        assertThat(result).isNotNull();
    }

    @Test
    void createAccountShouldFailWhenEmailExists() {
        CreateAccountRO ro = CreateAccountRO.builder().username("u").email("a@b.com").telephone("13800000000").build();
        when(accountMapper.exists(any(QueryWrapper.class))).thenReturn(false, true);

        String result = service.createAccount(ro);

        assertThat(result).isNotNull();
    }

    @Test
    void createAccountShouldFailWhenTelephoneExists() {
        CreateAccountRO ro = CreateAccountRO.builder().username("u").email("a@b.com").telephone("13800000000").build();
        when(accountMapper.exists(any(QueryWrapper.class))).thenReturn(false, false, true);

        String result = service.createAccount(ro);

        assertThat(result).isNotNull();
    }

    @Test
    void createAccountShouldSucceedWhenSaveSuccess() {
        CreateAccountRO ro = CreateAccountRO.builder().username("u").email("a@b.com").telephone("13800000000").build();
        when(accountMapper.exists(any(QueryWrapper.class))).thenReturn(false, false, false);
        when(encoder.encode("123456")).thenReturn("enc123456");
        doReturn(true).when(service).save(any(Account.class));

        String result = service.createAccount(ro);

        assertThat(result).isNull();
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(service).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("user");
        assertThat(captor.getValue().getGender()).isEqualTo(Gender.UNKNOWN);
    }

    @Test
    void createAccountShouldFailWhenSaveFailed() {
        CreateAccountRO ro = CreateAccountRO.builder().username("u").email("a@b.com").telephone("13800000000").build();
        when(accountMapper.exists(any(QueryWrapper.class))).thenReturn(false, false, false);
        when(encoder.encode("123456")).thenReturn("enc123456");
        doReturn(false).when(service).save(any(Account.class));

        String result = service.createAccount(ro);

        assertThat(result).isNotNull();
    }

    @Test
    void deleteAccountShouldFailWhenUserNotFound() {
        doReturn(null).when(service).getById(1L);

        String result = service.deleteAccount(1L, 99L);

        assertThat(result).isNotNull();
    }

    @Test
    void deleteAccountShouldFailWhenDeleteSelf() {
        doReturn(Account.builder().id(1L).build()).when(service).getById(1L);

        String result = service.deleteAccount(1L, 1L);

        assertThat(result).isNotNull();
    }

    @Test
    void deleteAccountShouldSucceedWhenUpdateSuccess() {
        doReturn(Account.builder().id(1L).build()).when(service).getById(1L);
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));

        String result = service.deleteAccount(1L, 2L);

        assertThat(result).isNull();
        verify(service).update(any(LambdaUpdateWrapper.class));
    }

    @Test
    void deleteAccountShouldFailWhenUpdateFailed() {
        doReturn(Account.builder().id(1L).build()).when(service).getById(1L);
        doReturn(false).when(service).update(any(LambdaUpdateWrapper.class));

        String result = service.deleteAccount(1L, 2L);

        assertThat(result).isNotNull();
    }

    @Test
    void updateAccountShouldFailWhenUserNotFound() {
        doReturn(null).when(service).getById(9L);

        String result = service.updateAccount(UpdateAccountRO.builder().username("new").build(), 9L);

        assertThat(result).isNotNull();
    }

    @Test
    void updateAccountShouldFailWhenUsernameTaken() {
        doReturn(Account.builder().id(9L).build()).when(service).getById(9L);
        when(accountMapper.exists(any(QueryWrapper.class))).thenReturn(true);

        String result = service.updateAccount(UpdateAccountRO.builder().username("new").build(), 9L);

        assertThat(result).isNotNull();
    }

    @Test
    void updateAccountShouldFailWhenTelephoneTaken() {
        doReturn(Account.builder().id(9L).build()).when(service).getById(9L);
        when(accountMapper.exists(any(QueryWrapper.class))).thenReturn(false, true);

        String result = service.updateAccount(UpdateAccountRO.builder().telephone("13900000000").build(), 9L);

        assertThat(result).isNotNull();
    }

    @Test
    void updateAccountShouldSucceedWhenUpdateByIdSuccess() {
        LocalDate birthday = LocalDate.of(2000, 1, 1);
        doReturn(Account.builder().id(9L).build()).when(service).getById(9L);
        when(accountMapper.exists(any(QueryWrapper.class))).thenReturn(false, false);
        doReturn(true).when(service).updateById(any(Account.class));

        String result = service.updateAccount(
                UpdateAccountRO.builder()
                        .username("new")
                        .gender(Gender.FEMALE)
                        .telephone("13900000000")
                        .birthday(birthday)
                        .build(),
                9L
        );

        assertThat(result).isNull();
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(service).updateById(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("new");
        assertThat(captor.getValue().getGender()).isEqualTo(Gender.FEMALE);
        assertThat(captor.getValue().getTelephone()).isEqualTo("13900000000");
        assertThat(captor.getValue().getBirthday()).isEqualTo(birthday);
    }

    @Test
    void updateAccountShouldFailWhenUpdateByIdFailed() {
        doReturn(Account.builder().id(9L).build()).when(service).getById(9L);
        doReturn(false).when(service).updateById(any(Account.class));

        String result = service.updateAccount(UpdateAccountRO.builder().build(), 9L);

        assertThat(result).isNotNull();
    }

    @Test
    void updateAccountShouldSkipUniqueCheckWhenUsernameAndTelephoneBlank() {
        doReturn(Account.builder().id(9L).build()).when(service).getById(9L);
        doReturn(true).when(service).updateById(any(Account.class));

        String result = service.updateAccount(
                UpdateAccountRO.builder().username("").telephone("").build(),
                9L
        );

        assertThat(result).isNull();
        verify(accountMapper, never()).exists(any(QueryWrapper.class));
    }

    @Test
    void updateAccountShouldUpdateOnlyGenderWhenOnlyGenderProvided() {
        doReturn(Account.builder().id(9L).build()).when(service).getById(9L);
        doReturn(true).when(service).updateById(any(Account.class));

        String result = service.updateAccount(
                UpdateAccountRO.builder().gender(Gender.MALE).build(),
                9L
        );

        assertThat(result).isNull();
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(service).updateById(captor.capture());
        assertThat(captor.getValue().getGender()).isEqualTo(Gender.MALE);
        assertThat(captor.getValue().getUsername()).isNull();
        assertThat(captor.getValue().getTelephone()).isNull();
        assertThat(captor.getValue().getBirthday()).isNull();
    }

    @Test
    void listAccountsShouldReturnMappedPage() {
        Account a1 = Account.builder()
                .id(1L)
                .username("u1")
                .email("u1@test.com")
                .telephone("13800000001")
                .role("user")
                .gender(Gender.MALE)
                .birthday(LocalDate.now().minusYears(20))
                .build();
        Account a2 = Account.builder()
                .id(2L)
                .username("u2")
                .email("u2@test.com")
                .telephone("13800000002")
                .role("user")
                .gender(Gender.FEMALE)
                .birthday(null)
                .build();
        IPage<Account> accountPage = new Page<>(2, 5, 12);
        accountPage.setRecords(List.of(a1, a2));
        doReturn(accountPage).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        IPage<AccountVO> result = service.listAccounts(ListAccountRO.builder().current(2L).size(5L).keyword("u").build());

        assertThat(result.getCurrent()).isEqualTo(2L);
        assertThat(result.getSize()).isEqualTo(5L);
        assertThat(result.getTotal()).isEqualTo(12L);
        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getRecords().get(0).getAge()).isNotNull();
        assertThat(result.getRecords().get(1).getAge()).isNull();
    }

    @Test
    void listAccountsShouldUseDefaultsWhenCurrentAndSizeAreNull() {
        IPage<Account> accountPage = new Page<>(1, 10, 0);
        accountPage.setRecords(List.of());
        doReturn(accountPage).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        IPage<AccountVO> result = service.listAccounts(ListAccountRO.builder().build());

        assertThat(result.getCurrent()).isEqualTo(1L);
        assertThat(result.getSize()).isEqualTo(10L);
    }

    @Test
    void listAccountsShouldHandleStartAndEndTime() {
        IPage<Account> accountPage = new Page<>(1, 10, 0);
        accountPage.setRecords(List.of());
        doReturn(accountPage).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        ListAccountRO ro = ListAccountRO.builder()
                .startTime(LocalDate.now().minusDays(7))
                .endTime(LocalDate.now())
                .build();
        IPage<AccountVO> result = service.listAccounts(ro);

        assertThat(result.getTotal()).isEqualTo(0L);
    }

    @Test
    void listAccountsShouldHandleOnlyStartTime() {
        IPage<Account> accountPage = new Page<>(1, 10, 0);
        accountPage.setRecords(List.of());
        doReturn(accountPage).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        ListAccountRO ro = ListAccountRO.builder()
                .startTime(LocalDate.now().minusDays(7))
                .build();
        IPage<AccountVO> result = service.listAccounts(ro);

        assertThat(result.getTotal()).isEqualTo(0L);
    }

    @Test
    void listAccountsShouldHandleOnlyEndTime() {
        IPage<Account> accountPage = new Page<>(1, 10, 0);
        accountPage.setRecords(List.of());
        doReturn(accountPage).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        ListAccountRO ro = ListAccountRO.builder()
                .endTime(LocalDate.now())
                .build();
        IPage<AccountVO> result = service.listAccounts(ro);

        assertThat(result.getTotal()).isEqualTo(0L);
    }

    @Test
    void listAccountsShouldIgnoreBlankKeyword() {
        IPage<Account> accountPage = new Page<>(1, 10, 0);
        accountPage.setRecords(List.of());
        doReturn(accountPage).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        IPage<AccountVO> result = service.listAccounts(ListAccountRO.builder().keyword("   ").build());

        assertThat(result.getTotal()).isEqualTo(0L);
    }

    @Test
    void getAccountInfoShouldReturnNullWhenUserMissing() {
        doReturn(null).when(service).getById(1L);

        AccountVO result = service.getAccountInfo(1L);

        assertThat(result).isNull();
    }

    @Test
    void getAccountInfoShouldReturnMappedVoWhenUserExists() {
        doReturn(Account.builder()
                .id(1L)
                .username("u")
                .birthday(LocalDate.now().minusYears(18))
                .build()).when(service).getById(1L);

        AccountVO result = service.getAccountInfo(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("u");
        assertThat(result.getAge()).isEqualTo(18);
    }

    @Test
    void updateAvatarShouldFailWhenUserMissing() {
        doReturn(null).when(service).getById(1L);

        String result = service.updateAvatar(1L, "/a.png");

        assertThat(result).isNotNull();
    }

    @Test
    void updateAvatarShouldSucceedWhenUpdateByIdSuccess() {
        doReturn(Account.builder().id(1L).build()).when(service).getById(1L);
        doReturn(true).when(service).updateById(any(Account.class));

        String result = service.updateAvatar(1L, "/a.png");

        assertThat(result).isNull();
    }

    @Test
    void updateAvatarShouldFailWhenUpdateByIdFailed() {
        doReturn(Account.builder().id(1L).build()).when(service).getById(1L);
        doReturn(false).when(service).updateById(any(Account.class));

        String result = service.updateAvatar(1L, "/a.png");

        assertThat(result).isNotNull();
    }

    @Test
    void initDefaultAdminShouldSkipWhenCountNotZero() {
        doReturn(1L).when(service).count();

        service.initDefaultAdmin();

        verify(service, times(0)).save(any(Account.class));
    }

    @Test
    void initDefaultAdminShouldCreateAdminAndTestsWhenTableEmpty() {
        doReturn(0L).when(service).count();
        when(encoder.encode("123456")).thenReturn("enc");
        doReturn(true).when(service).save(any(Account.class));

        service.initDefaultAdmin();

        verify(service, times(4)).save(any(Account.class));
    }

    @Test
    void initDefaultAdminShouldStillTryAllAccountsWhenSaveFails() {
        doReturn(0L).when(service).count();
        when(encoder.encode("123456")).thenReturn("enc");
        doReturn(false).when(service).save(any(Account.class));

        service.initDefaultAdmin();

        verify(service, times(4)).save(any(Account.class));
    }
}
