package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ynu.jackielinn.server.entity.Round;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundServiceImplTest {

    @Spy
    @InjectMocks
    private RoundServiceImpl service;

    @Test
    void saveRoundShouldDelegateToSave() {
        Round round = Round.builder().tid(1L).roundNum(1).build();
        doReturn(true).when(service).save(round);

        service.saveRound(round);

        verify(service).save(round);
    }

    @Test
    void getByTidAndRoundNumShouldReturnOneFromChain() {
        LambdaQueryChainWrapper<Round> chain = mock(LambdaQueryChainWrapper.class);
        Round target = Round.builder().id(10L).tid(1L).roundNum(2).build();
        doReturn(chain).when(service).lambdaQuery();
        when(chain.eq(anyRoundFn(), any())).thenReturn(chain);
        when(chain.last(anyString())).thenReturn(chain);
        when(chain.one()).thenReturn(target);

        Round result = service.getByTidAndRoundNum(1L, 2);

        assertThat(result).isSameAs(target);
    }

    @Test
    void listByTidOrderByRoundNumShouldReturnListFromChain() {
        LambdaQueryChainWrapper<Round> chain = mock(LambdaQueryChainWrapper.class);
        List<Round> rounds = List.of(
                Round.builder().id(1L).tid(9L).roundNum(1).build(),
                Round.builder().id(2L).tid(9L).roundNum(2).build()
        );
        doReturn(chain).when(service).lambdaQuery();
        when(chain.eq(anyRoundFn(), any())).thenReturn(chain);
        when(chain.orderByAsc(anyRoundFn())).thenReturn(chain);
        when(chain.list()).thenReturn(rounds);

        List<Round> result = service.listByTidOrderByRoundNum(9L);

        assertThat(result).containsExactlyElementsOf(rounds);
    }

    private static <R> SFunction<Round, R> anyRoundFn() {
        return (SFunction<Round, R>) any(SFunction.class);
    }
}
