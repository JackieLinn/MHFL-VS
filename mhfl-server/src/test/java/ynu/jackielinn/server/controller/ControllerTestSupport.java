package ynu.jackielinn.server.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ynu.jackielinn.server.utils.JwtUtils;

@AutoConfigureMockMvc(addFilters = false)
public abstract class ControllerTestSupport {

    @MockitoBean
    protected JwtUtils jwtUtils;

    @MockitoBean
    protected StringRedisTemplate stringRedisTemplate;
}
