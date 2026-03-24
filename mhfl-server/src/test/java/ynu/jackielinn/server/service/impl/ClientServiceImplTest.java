package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ynu.jackielinn.server.entity.Client;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Spy
    @InjectMocks
    private ClientServiceImpl service;

    @Test
    void saveClientShouldDelegateToSave() {
        Client client = Client.builder().rid(1L).clientIndex(0).build();
        doReturn(true).when(service).save(client);

        service.saveClient(client);

        verify(service).save(client);
    }

    @Test
    void getLatestByRidsAndClientIndexShouldReturnNullWhenRidsEmpty() {
        assertThat(service.getLatestByRidsAndClientIndex(null, 0)).isNull();
        assertThat(service.getLatestByRidsAndClientIndex(List.of(), 0)).isNull();
    }

    @Test
    void getLatestByRidsAndClientIndexShouldReturnOneFromChain() {
        LambdaQueryChainWrapper<Client> chain = mock(LambdaQueryChainWrapper.class);
        Client latest = Client.builder().id(11L).clientIndex(3).timestamp(LocalDateTime.now()).build();
        doReturn(chain).when(service).lambdaQuery();
        when(chain.in(anyClientFn(), org.mockito.ArgumentMatchers.<Long>anyList())).thenReturn(chain);
        when(chain.eq(anyClientFn(), any())).thenReturn(chain);
        when(chain.orderByDesc(anyClientFn())).thenReturn(chain);
        when(chain.last(anyString())).thenReturn(chain);
        when(chain.one()).thenReturn(latest);

        Client result = service.getLatestByRidsAndClientIndex(List.of(1L, 2L), 3);

        assertThat(result).isSameAs(latest);
    }

    @Test
    void listByRidsAndClientIndexShouldReturnEmptyWhenRidsEmpty() {
        assertThat(service.listByRidsAndClientIndex(null, 1)).isEmpty();
        assertThat(service.listByRidsAndClientIndex(List.of(), 1)).isEmpty();
    }

    @Test
    void listByRidsAndClientIndexShouldReturnListFromChain() {
        LambdaQueryChainWrapper<Client> chain = mock(LambdaQueryChainWrapper.class);
        List<Client> clients = List.of(
                Client.builder().id(1L).rid(8L).clientIndex(2).build(),
                Client.builder().id(2L).rid(9L).clientIndex(2).build()
        );
        doReturn(chain).when(service).lambdaQuery();
        when(chain.in(anyClientFn(), org.mockito.ArgumentMatchers.<Long>anyList())).thenReturn(chain);
        when(chain.eq(anyClientFn(), any())).thenReturn(chain);
        when(chain.orderByAsc(anyClientFn())).thenReturn(chain);
        when(chain.list()).thenReturn(clients);

        List<Client> result = service.listByRidsAndClientIndex(List.of(8L, 9L), 2);

        assertThat(result).containsExactlyElementsOf(clients);
    }

    @Test
    void listByRidInShouldReturnEmptyWhenRidsEmpty() {
        assertThat(service.listByRidIn(null)).isEmpty();
        assertThat(service.listByRidIn(List.of())).isEmpty();
    }

    @Test
    void listByRidInShouldReturnListFromChain() {
        LambdaQueryChainWrapper<Client> chain = mock(LambdaQueryChainWrapper.class);
        List<Client> clients = List.of(
                Client.builder().id(3L).rid(10L).build(),
                Client.builder().id(4L).rid(11L).build()
        );
        doReturn(chain).when(service).lambdaQuery();
        when(chain.in(anyClientFn(), org.mockito.ArgumentMatchers.<Long>anyList())).thenReturn(chain);
        when(chain.list()).thenReturn(clients);

        List<Client> result = service.listByRidIn(List.of(10L, 11L));

        assertThat(result).containsExactlyElementsOf(clients);
    }

    private static <R> SFunction<Client, R> anyClientFn() {
        return (SFunction<Client, R>) any(SFunction.class);
    }
}
