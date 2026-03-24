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
import ynu.jackielinn.server.dto.request.ListDatasetRO;
import ynu.jackielinn.server.dto.response.DatasetVO;
import ynu.jackielinn.server.entity.Dataset;
import ynu.jackielinn.server.mapper.DatasetMapper;

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
class DatasetServiceImplTest {

    @Spy
    @InjectMocks
    private DatasetServiceImpl service;

    @Mock
    private DatasetMapper datasetMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", datasetMapper);
    }

    @Test
    void createDatasetShouldReturnMessageWhenNameExists() {
        QueryChainWrapper<Dataset> chain = mock(QueryChainWrapper.class);
        doReturn(chain).when(service).query();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.exists()).thenReturn(true);

        String result = service.createDataset("CIFAR-100");

        assertThat(result).isNotNull();
        verify(service, never()).save(any(Dataset.class));
    }

    @Test
    void createDatasetShouldReturnNullWhenSaveSuccess() {
        QueryChainWrapper<Dataset> chain = mock(QueryChainWrapper.class);
        doReturn(chain).when(service).query();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.exists()).thenReturn(false);
        doReturn(true).when(service).save(any(Dataset.class));

        String result = service.createDataset("CIFAR-100");

        assertThat(result).isNull();
        ArgumentCaptor<Dataset> captor = ArgumentCaptor.forClass(Dataset.class);
        verify(service).save(captor.capture());
        assertThat(captor.getValue().getDataName()).isEqualTo("CIFAR-100");
    }

    @Test
    void createDatasetShouldReturnMessageWhenSaveFailed() {
        QueryChainWrapper<Dataset> chain = mock(QueryChainWrapper.class);
        doReturn(chain).when(service).query();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.exists()).thenReturn(false);
        doReturn(false).when(service).save(any(Dataset.class));

        String result = service.createDataset("CIFAR-100");

        assertThat(result).isNotNull();
    }

    @Test
    void deleteDatasetShouldCoverBranches() {
        doReturn(null).when(service).getById(1L);
        assertThat(service.deleteDataset(1L)).isNotNull();

        doReturn(Dataset.builder().id(2L).build()).when(service).getById(2L);
        doReturn(false, true).when(service).update(any(LambdaUpdateWrapper.class));
        assertThat(service.deleteDataset(2L)).isNotNull();
        assertThat(service.deleteDataset(2L)).isNull();
    }

    @Test
    void updateDatasetShouldCoverBranches() {
        doReturn(null).when(service).getById(1L);
        assertThat(service.updateDataset(1L, "A")).isNotNull();

        doReturn(Dataset.builder().id(2L).dataName("old").build()).when(service).getById(2L);
        QueryChainWrapper<Dataset> chain = mock(QueryChainWrapper.class);
        doReturn(chain).when(service).query();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.ne(anyString(), any())).thenReturn(chain);
        when(chain.exists()).thenReturn(true, false);

        assertThat(service.updateDataset(2L, "new")).isNotNull();

        doReturn(false, true).when(service).updateById(any(Dataset.class));
        assertThat(service.updateDataset(2L, "new")).isNotNull();
        assertThat(service.updateDataset(2L, "new")).isNull();
    }

    @Test
    void listDatasetsShouldUsePageWhenAllFalse() {
        IPage<Dataset> pageResult = new Page<>(2, 5, 2);
        pageResult.setRecords(List.of(
                Dataset.builder().id(1L).dataName("D1").build(),
                Dataset.builder().id(2L).dataName("D2").build()
        ));
        doReturn(pageResult).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        IPage<DatasetVO> result = service.listDatasets(ListDatasetRO.builder()
                .all(false)
                .current(2L)
                .size(5L)
                .keyword(" D ")
                .startTime(LocalDate.now().minusDays(2))
                .endTime(LocalDate.now())
                .build());

        assertThat(result.getCurrent()).isEqualTo(2L);
        assertThat(result.getSize()).isEqualTo(5L);
        assertThat(result.getTotal()).isEqualTo(2L);
        assertThat(result.getRecords()).extracting(DatasetVO::getDataName).containsExactly("D1", "D2");
        verify(service, never()).list(any(LambdaQueryWrapper.class));
    }

    @Test
    void listDatasetsShouldUseDefaultPageWhenCurrentAndSizeNull() {
        IPage<Dataset> pageResult = new Page<>(1, 10, 0);
        pageResult.setRecords(List.of());
        doReturn(pageResult).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        ListDatasetRO ro = new ListDatasetRO();
        ro.setAll(false);
        ro.setCurrent(null);
        ro.setSize(null);

        IPage<DatasetVO> result = service.listDatasets(ro);

        assertThat(result.getCurrent()).isEqualTo(1L);
        assertThat(result.getSize()).isEqualTo(10L);
        assertThat(result.getTotal()).isEqualTo(0L);
    }

    @Test
    void listDatasetsShouldReturnAllWhenAllTrue() {
        doReturn(List.of(
                Dataset.builder().id(1L).dataName("D1").build(),
                Dataset.builder().id(2L).dataName("D2").build()
        )).when(service).list(any(LambdaQueryWrapper.class));

        IPage<DatasetVO> result = service.listDatasets(ListDatasetRO.builder()
                .all(true)
                .keyword("D")
                .build());

        assertThat(result.getCurrent()).isEqualTo(1L);
        assertThat(result.getTotal()).isEqualTo(2L);
        assertThat(result.getSize()).isEqualTo(2L);
        assertThat(result.getRecords()).hasSize(2);
        verify(service, never()).page(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void listDatasetsShouldCoverOnlyStartOnlyEndAndBlankKeyword() {
        IPage<Dataset> pageResult = new Page<>(1, 10, 0);
        pageResult.setRecords(List.of());
        doReturn(pageResult).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        IPage<DatasetVO> onlyStart = service.listDatasets(ListDatasetRO.builder()
                .startTime(LocalDate.now().minusDays(1))
                .build());
        IPage<DatasetVO> onlyEnd = service.listDatasets(ListDatasetRO.builder()
                .endTime(LocalDate.now())
                .build());
        IPage<DatasetVO> blankKeyword = service.listDatasets(ListDatasetRO.builder()
                .keyword("   ")
                .build());

        assertThat(onlyStart.getTotal()).isEqualTo(0L);
        assertThat(onlyEnd.getTotal()).isEqualTo(0L);
        assertThat(blankKeyword.getTotal()).isEqualTo(0L);
    }
}

