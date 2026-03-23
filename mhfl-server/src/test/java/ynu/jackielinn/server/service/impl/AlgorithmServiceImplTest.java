package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ynu.jackielinn.server.dto.request.ListAlgorithmRO;
import ynu.jackielinn.server.dto.response.AlgorithmVO;
import ynu.jackielinn.server.entity.Algorithm;
import ynu.jackielinn.server.mapper.AlgorithmMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlgorithmServiceImplTest {

    @Spy
    @InjectMocks
    private AlgorithmServiceImpl service;

    @Mock
    private AlgorithmMapper algorithmMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", algorithmMapper);
    }

    @Test
    void createAlgorithmShouldReturnMessageWhenNameExists() {
        QueryChainWrapper<Algorithm> chain = mock(QueryChainWrapper.class);
        doReturn(chain).when(service).query();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.exists()).thenReturn(true);

        String result = service.createAlgorithm("FedAvg");

        assertThat(result).isNotNull();
        verify(service, never()).save(any(Algorithm.class));
    }

    @Test
    void createAlgorithmShouldReturnNullWhenSaveSuccess() {
        QueryChainWrapper<Algorithm> chain = mock(QueryChainWrapper.class);
        doReturn(chain).when(service).query();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.exists()).thenReturn(false);
        doReturn(true).when(service).save(any(Algorithm.class));

        String result = service.createAlgorithm("FedAvg");

        assertThat(result).isNull();
        ArgumentCaptor<Algorithm> captor = ArgumentCaptor.forClass(Algorithm.class);
        verify(service).save(captor.capture());
        assertThat(captor.getValue().getAlgorithmName()).isEqualTo("FedAvg");
    }

    @Test
    void createAlgorithmShouldReturnMessageWhenSaveFailed() {
        QueryChainWrapper<Algorithm> chain = mock(QueryChainWrapper.class);
        doReturn(chain).when(service).query();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.exists()).thenReturn(false);
        doReturn(false).when(service).save(any(Algorithm.class));

        String result = service.createAlgorithm("FedAvg");

        assertThat(result).isNotNull();
    }

    @Test
    void deleteAlgorithmShouldReturnMessageWhenNotFound() {
        doReturn(null).when(service).getById(1L);

        String result = service.deleteAlgorithm(1L);

        assertThat(result).isNotNull();
        verify(service, never()).update(any(LambdaUpdateWrapper.class));
    }

    @Test
    void deleteAlgorithmShouldReturnNullWhenUpdateSuccess() {
        doReturn(Algorithm.builder().id(1L).algorithmName("A").build()).when(service).getById(1L);
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));

        String result = service.deleteAlgorithm(1L);

        assertThat(result).isNull();
    }

    @Test
    void deleteAlgorithmShouldReturnMessageWhenUpdateFailed() {
        doReturn(Algorithm.builder().id(1L).algorithmName("A").build()).when(service).getById(1L);
        doReturn(false).when(service).update(any(LambdaUpdateWrapper.class));

        String result = service.deleteAlgorithm(1L);

        assertThat(result).isNotNull();
    }

    @Test
    void updateAlgorithmShouldReturnMessageWhenNotFound() {
        doReturn(null).when(service).getById(1L);

        String result = service.updateAlgorithm(1L, "NewAlgo");

        assertThat(result).isNotNull();
    }

    @Test
    void updateAlgorithmShouldReturnMessageWhenNameUsedByOther() {
        doReturn(Algorithm.builder().id(1L).algorithmName("Old").build()).when(service).getById(1L);
        QueryChainWrapper<Algorithm> chain = mock(QueryChainWrapper.class);
        doReturn(chain).when(service).query();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.ne(anyString(), any())).thenReturn(chain);
        when(chain.exists()).thenReturn(true);

        String result = service.updateAlgorithm(1L, "NewAlgo");

        assertThat(result).isNotNull();
        verify(service, never()).updateById(any(Algorithm.class));
    }

    @Test
    void updateAlgorithmShouldReturnNullWhenUpdateByIdSuccess() {
        doReturn(Algorithm.builder().id(1L).algorithmName("Old").build()).when(service).getById(1L);
        QueryChainWrapper<Algorithm> chain = mock(QueryChainWrapper.class);
        doReturn(chain).when(service).query();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.ne(anyString(), any())).thenReturn(chain);
        when(chain.exists()).thenReturn(false);
        doReturn(true).when(service).updateById(any(Algorithm.class));

        String result = service.updateAlgorithm(1L, "NewAlgo");

        assertThat(result).isNull();
        ArgumentCaptor<Algorithm> captor = ArgumentCaptor.forClass(Algorithm.class);
        verify(service).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getAlgorithmName()).isEqualTo("NewAlgo");
    }

    @Test
    void updateAlgorithmShouldReturnMessageWhenUpdateByIdFailed() {
        doReturn(Algorithm.builder().id(1L).algorithmName("Old").build()).when(service).getById(1L);
        QueryChainWrapper<Algorithm> chain = mock(QueryChainWrapper.class);
        doReturn(chain).when(service).query();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.ne(anyString(), any())).thenReturn(chain);
        when(chain.exists()).thenReturn(false);
        doReturn(false).when(service).updateById(any(Algorithm.class));

        String result = service.updateAlgorithm(1L, "NewAlgo");

        assertThat(result).isNotNull();
    }

    @Test
    void listAlgorithmsShouldReturnPagedResultWhenAllFalse() {
        Algorithm a1 = Algorithm.builder().id(1L).algorithmName("FedAvg").build();
        Algorithm a2 = Algorithm.builder().id(2L).algorithmName("FedProto").build();
        IPage<Algorithm> pageResult = new Page<>(2, 5, 12);
        pageResult.setRecords(List.of(a1, a2));
        doReturn(pageResult).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        IPage<AlgorithmVO> result = service.listAlgorithms(ListAlgorithmRO.builder()
                .current(2L)
                .size(5L)
                .keyword(" Fed ")
                .build());

        assertThat(result.getCurrent()).isEqualTo(2L);
        assertThat(result.getSize()).isEqualTo(5L);
        assertThat(result.getTotal()).isEqualTo(12L);
        assertThat(result.getRecords()).extracting(AlgorithmVO::getAlgorithmName).containsExactly("FedAvg", "FedProto");
        verify(service, never()).list(any(LambdaQueryWrapper.class));
    }

    @Test
    void listAlgorithmsShouldUseDefaultPageWhenCurrentAndSizeNull() {
        IPage<Algorithm> pageResult = new Page<>(1, 10, 0);
        pageResult.setRecords(List.of());
        doReturn(pageResult).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        IPage<AlgorithmVO> result = service.listAlgorithms(ListAlgorithmRO.builder().all(false).build());

        assertThat(result.getCurrent()).isEqualTo(1L);
        assertThat(result.getSize()).isEqualTo(10L);
        verify(service, never()).list(any(LambdaQueryWrapper.class));
    }

    @Test
    void listAlgorithmsShouldReturnAllWhenAllTrue() {
        Algorithm a1 = Algorithm.builder().id(1L).algorithmName("FedAvg").build();
        Algorithm a2 = Algorithm.builder().id(2L).algorithmName("FedProto").build();
        doReturn(List.of(a1, a2)).when(service).list(any(LambdaQueryWrapper.class));

        IPage<AlgorithmVO> result = service.listAlgorithms(ListAlgorithmRO.builder()
                .all(true)
                .keyword("Fed")
                .build());

        assertThat(result.getCurrent()).isEqualTo(1L);
        assertThat(result.getTotal()).isEqualTo(2L);
        assertThat(result.getSize()).isEqualTo(2L);
        assertThat(result.getRecords()).hasSize(2);
        verify(service, never()).page(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void listAlgorithmsShouldHandleTimeRangeBranches() {
        IPage<Algorithm> pageResult = new Page<>(1, 10, 0);
        pageResult.setRecords(List.of());
        doReturn(pageResult).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        IPage<AlgorithmVO> both = service.listAlgorithms(ListAlgorithmRO.builder()
                .startTime(LocalDate.now().minusDays(7))
                .endTime(LocalDate.now())
                .build());
        IPage<AlgorithmVO> onlyStart = service.listAlgorithms(ListAlgorithmRO.builder()
                .startTime(LocalDate.now().minusDays(7))
                .build());
        IPage<AlgorithmVO> onlyEnd = service.listAlgorithms(ListAlgorithmRO.builder()
                .endTime(LocalDate.now())
                .build());
        IPage<AlgorithmVO> blankKeyword = service.listAlgorithms(ListAlgorithmRO.builder()
                .keyword("   ")
                .build());

        assertThat(both.getTotal()).isEqualTo(0L);
        assertThat(onlyStart.getTotal()).isEqualTo(0L);
        assertThat(onlyEnd.getTotal()).isEqualTo(0L);
        assertThat(blankKeyword.getTotal()).isEqualTo(0L);
    }
}
