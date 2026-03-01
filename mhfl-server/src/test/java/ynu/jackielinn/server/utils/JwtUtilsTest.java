package ynu.jackielinn.server.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * JwtUtils 单元测试，覆盖创建/解析/失效 Token、convertToken、expireTime、toUser、toId。
 */
@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    private static final String KEY = "testSecretKeyForJwtSigningAtLeast32BytesLong";
    private static final int EXPIRE_DAYS = 7;

    @Mock
    private StringRedisTemplate template;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtils, "key", KEY);
        ReflectionTestUtils.setField(jwtUtils, "expire", EXPIRE_DAYS);
        // lenient: 部分用例只调用 hasKey 不调用 opsForValue，避免 UnnecessaryStubbingException
        lenient().when(template.opsForValue()).thenReturn(valueOperations);
    }

    // ---------- convertToken ----------
    @Test
    void convertTokenShouldReturnNullWhenHeaderTokenIsNull() {
        assertNull(jwtUtils.convertToken(null));
    }

    @Test
    void convertTokenShouldReturnNullWhenHeaderTokenDoesNotStartWithBearer() {
        assertNull(jwtUtils.convertToken("NoPrefix"));
        assertNull(jwtUtils.convertToken(""));
        assertNull(jwtUtils.convertToken("Bearer")); // no space
    }

    @Test
    void convertTokenShouldReturnEmptyStringWhenOnlyBearerPrefix() {
        assertEquals("", jwtUtils.convertToken("Bearer "));
    }

    @Test
    void convertTokenShouldReturnTokenWithoutBearerPrefix() {
        assertEquals("abc", jwtUtils.convertToken("Bearer abc"));
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xxx", jwtUtils.convertToken("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xxx"));
    }

    // ---------- expireTime ----------
    @Test
    void expireTimeShouldReturnDateInFuture() {
        Date before = new Date();
        Date expire = jwtUtils.expireTime();
        Date after = new Date();
        assertTrue(expire.after(before) && expire.after(after));
        long expectedMin = System.currentTimeMillis() + (EXPIRE_DAYS * 24L * 3600 * 1000) - 60_000;
        long expectedMax = System.currentTimeMillis() + (EXPIRE_DAYS * 24L * 3600 * 1000) + 60_000;
        assertTrue(expire.getTime() >= expectedMin && expire.getTime() <= expectedMax);
    }

    // ---------- createJwt ----------
    @Test
    void createJwtShouldReturnNonEmptyToken() {
        UserDetails user = User.builder()
                .username("testuser")
                .password("******")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_user")))
                .build();
        String token = jwtUtils.createJwt(user, 100L, "testuser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void createJwtShouldProduceTokenDecodableByResolveJwt() {
        UserDetails user = User.builder()
                .username("decodeUser")
                .password("******")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_admin")))
                .build();
        String token = jwtUtils.createJwt(user, 200L, "decodeUser");
        when(template.hasKey(anyString())).thenReturn(false);

        DecodedJWT decoded = jwtUtils.resolveJwt("Bearer " + token);
        assertNotNull(decoded);
        assertEquals(200L, jwtUtils.toId(decoded));
        UserDetails fromJwt = jwtUtils.toUser(decoded);
        assertEquals("decodeUser", fromJwt.getUsername());
        assertTrue(fromJwt.getAuthorities().stream().anyMatch(a -> "ROLE_admin".equals(a.getAuthority())));
    }

    // ---------- resolveJwt ----------
    @Test
    void resolveJwtShouldReturnNullWhenHeaderTokenIsNull() {
        assertNull(jwtUtils.resolveJwt(null));
    }

    @Test
    void resolveJwtShouldReturnNullWhenTokenIsEmptyAfterBearer() {
        assertNull(jwtUtils.resolveJwt("Bearer "));
    }

    @Test
    void resolveJwtShouldReturnNullWhenNoBearerPrefix() {
        assertNull(jwtUtils.resolveJwt("invalid"));
    }

    @Test
    void resolveJwtShouldReturnNullWhenTokenIsInvalid() {
        assertNull(jwtUtils.resolveJwt("Bearer invalid.jwt.token"));
    }

    @Test
    void resolveJwtShouldReturnNullWhenTokenIsBlacklisted() {
        UserDetails user = User.builder().username("u").password("p").authorities(Collections.emptyList()).build();
        String token = jwtUtils.createJwt(user, 1L, "u");
        when(template.hasKey(startsWith(Const.JWT_BLACK_LIST))).thenReturn(true);

        assertNull(jwtUtils.resolveJwt("Bearer " + token));
    }

    @Test
    void resolveJwtShouldReturnNullWhenTokenIsExpired() {
        Algorithm algorithm = Algorithm.HMAC256(KEY);
        Date past = new Date(System.currentTimeMillis() - 86400_000);
        String expiredToken = JWT.create()
                .withJWTId("expired-id")
                .withClaim("id", 999L)
                .withClaim("username", "expired")
                .withClaim("authorities", Collections.<String>emptyList())
                .withExpiresAt(past)
                .withIssuedAt(new Date(System.currentTimeMillis() - 86400_000 * 2))
                .sign(algorithm);
        // 过期 token 在 verify() 时即抛异常，不会走到 isInvalidToken，故无需 stub hasKey

        assertNull(jwtUtils.resolveJwt("Bearer " + expiredToken));
    }

    @Test
    void resolveJwtShouldReturnDecodedJwtWhenValidAndNotBlacklisted() {
        UserDetails user = User.builder().username("v").password("p").authorities(Collections.emptyList()).build();
        String token = jwtUtils.createJwt(user, 42L, "v");
        when(template.hasKey(anyString())).thenReturn(false);

        DecodedJWT decoded = jwtUtils.resolveJwt("Bearer " + token);
        assertNotNull(decoded);
        assertEquals(42L, jwtUtils.toId(decoded));
        assertEquals("v", jwtUtils.toUser(decoded).getUsername());
    }

    // ---------- invalidateJwt ----------
    @Test
    void invalidateJwtShouldReturnFalseWhenHeaderTokenIsNull() {
        assertFalse(jwtUtils.invalidateJwt(null));
    }

    @Test
    void invalidateJwtShouldReturnFalseWhenTokenEmptyAfterBearer() {
        assertFalse(jwtUtils.invalidateJwt("Bearer "));
    }

    @Test
    void invalidateJwtShouldReturnFalseWhenTokenInvalid() {
        assertFalse(jwtUtils.invalidateJwt("Bearer bad.token.here"));
    }

    @Test
    void invalidateJwtShouldReturnFalseWhenTokenAlreadyBlacklisted() {
        UserDetails user = User.builder().username("u").password("p").authorities(Collections.emptyList()).build();
        String token = jwtUtils.createJwt(user, 1L, "u");
        when(template.hasKey(startsWith(Const.JWT_BLACK_LIST))).thenReturn(true);

        assertFalse(jwtUtils.invalidateJwt("Bearer " + token));
    }

    @Test
    void invalidateJwtShouldReturnTrueAndSetBlacklistWhenTokenValidAndNotBlacklisted() {
        UserDetails user = User.builder().username("u").password("p").authorities(Collections.emptyList()).build();
        String token = jwtUtils.createJwt(user, 1L, "u");
        when(template.hasKey(anyString())).thenReturn(false);

        boolean result = jwtUtils.invalidateJwt("Bearer " + token);

        assertTrue(result);
        verify(template).opsForValue();
        verify(valueOperations).set(startsWith(Const.JWT_BLACK_LIST), eq(""), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    // ---------- toUser / toId ----------
    @Test
    void toUserShouldReturnUserDetailsWithUsernameAndAuthorities() {
        UserDetails user = User.builder()
                .username("toUser")
                .password("******")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_user"), new SimpleGrantedAuthority("ROLE_admin")))
                .build();
        String token = jwtUtils.createJwt(user, 10L, "toUser");
        when(template.hasKey(anyString())).thenReturn(false);
        DecodedJWT decoded = jwtUtils.resolveJwt("Bearer " + token);
        assertNotNull(decoded);

        UserDetails fromJwt = jwtUtils.toUser(decoded);
        assertEquals("toUser", fromJwt.getUsername());
        assertEquals(2, fromJwt.getAuthorities().size());
    }

    @Test
    void toIdShouldReturnUserIdFromJwt() {
        UserDetails user = User.builder().username("idUser").password("p").authorities(Collections.emptyList()).build();
        String token = jwtUtils.createJwt(user, 999L, "idUser");
        when(template.hasKey(anyString())).thenReturn(false);
        DecodedJWT decoded = jwtUtils.resolveJwt("Bearer " + token);
        assertNotNull(decoded);
        assertEquals(999L, jwtUtils.toId(decoded));
    }
}
