package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.dto.request.ListDatasetRO;
import ynu.jackielinn.server.dto.response.DatasetVO;
import ynu.jackielinn.server.entity.Dataset;
import ynu.jackielinn.server.mapper.DatasetMapper;
import ynu.jackielinn.server.service.DatasetService;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class DatasetServiceImpl extends ServiceImpl<DatasetMapper, Dataset> implements DatasetService {

    /**
     * 管理员创建数据集，需要检查数据集名字是否存在重名
     *
     * @param dataName 数据集名字
     * @return null 表示成功，否则为错误信息
     */
    @Override
    public String createDataset(String dataName) {
        if (this.existsDatasetByDataName(dataName)) {
            return "此数据集名字已存在，请更换一个新的名字";
        }
        Dataset dataset = Dataset.builder()
                .dataName(dataName)
                .build();
        if (this.save(dataset)) {
            return null;
        } else {
            return "内部错误，请联系管理员";
        }
    }

    /**
     * 管理员逻辑删除数据集（手动设置 is_deleted = 1 和 delete_time）
     * 由于 deleted 字段使用了 @TableLogic 注解，updateById 不会更新该字段
     * 因此使用 LambdaUpdateWrapper 显式更新 is_deleted 和 delete_time
     * update_time 会通过 updateFill 自动更新
     *
     * @param id 要删除的数据集 id
     * @return null 表示成功，否则为错误信息
     */
    @Override
    public String deleteDataset(Long id) {
        Dataset dataset = this.getById(id);
        if (dataset == null) {
            return "数据集不存在";
        }
        // 使用 LambdaUpdateWrapper 显式更新 is_deleted 和 delete_time
        // 由于 deleted 字段使用了 @TableLogic，updateById 不会更新该字段
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Dataset> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Dataset::getId, id)
                .set(Dataset::getDeleted, 1)
                .set(Dataset::getDeleteTime, now);
        if (this.update(updateWrapper)) {
            return null;
        }
        return "删除失败，请联系管理员";
    }

    /**
     * 管理员更新数据集名字，需要检查数据集名字是否已被其他数据集使用（排除当前数据集）
     *
     * @param id       要更新的数据集 id
     * @param dataName 新的数据集名字
     * @return null 表示成功，否则为错误信息
     */
    @Override
    public String updateDataset(Long id, String dataName) {
        Dataset dataset = this.getById(id);
        if (dataset == null) {
            return "数据集不存在";
        }

        // 检查数据集名字唯一性（排除当前数据集）
        if (existsDatasetByDataNameExcludeId(dataName, id)) {
            return "此数据集名字已被其他数据集使用，请更换一个新的名字";
        }

        // 使用 LambdaUpdateWrapper 更新数据集名字
        LambdaUpdateWrapper<Dataset> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Dataset::getId, id)
                .set(Dataset::getDataName, dataName);
        if (this.update(updateWrapper)) {
            return null;
        } else {
            return "更新失败，请联系管理员";
        }
    }

    /**
     * 管理员查询数据集列表（支持关键字模糊查询和时间范围查询，分页）
     * 关键字为空则查询全部，不为空则在数据集名字中模糊查询
     *
     * @param ro 查询条件对象
     * @return 分页结果（DatasetVO，排除敏感信息）
     */
    @Override
    public IPage<DatasetVO> listDatasets(ListDatasetRO ro) {
        // 处理默认值（@ModelAttribute 绑定可能不会应用 @Builder.Default）
        long current = ro.getCurrent() != null ? ro.getCurrent() : 1L;
        long size = ro.getSize() != null ? ro.getSize() : 10L;
        Page<Dataset> page = new Page<>(current, size);
        LambdaQueryWrapper<Dataset> wrapper = new LambdaQueryWrapper<>();

        // 关键字模糊查询（数据集名字）
        if (ro.getKeyword() != null && !ro.getKeyword().trim().isEmpty()) {
            String keywordTrimmed = ro.getKeyword().trim();
            wrapper.like(Dataset::getDataName, keywordTrimmed);
        }

        // 创建时间范围查询（将 LocalDate 转换为 LocalDateTime）
        LocalDate startDate = ro.getStartTime();
        LocalDate endDate = ro.getEndTime();
        if (startDate != null && endDate != null) {
            // 两个时间都有：时间范围内（起始时间当天 00:00:00 到终止时间当天 23:59:59）
            LocalDateTime startTime = startDate.atStartOfDay();
            LocalDateTime endTime = endDate.atTime(23, 59, 59);
            wrapper.between(Dataset::getCreateTime, startTime, endTime);
        } else if (startDate != null) {
            // 起始时间有，终止时间空：从起始时间当天 00:00:00 到现在
            LocalDateTime startTime = startDate.atStartOfDay();
            wrapper.ge(Dataset::getCreateTime, startTime);
        } else if (endDate != null) {
            // 起始时间空，终止时间有：终止时间当天 23:59:59 之前
            LocalDateTime endTime = endDate.atTime(23, 59, 59);
            wrapper.le(Dataset::getCreateTime, endTime);
        }
        // 两个时间都空：不限时间（不添加条件）

        // 按 id 从小到大排序
        wrapper.orderByAsc(Dataset::getId);

        // 查询Dataset分页结果
        IPage<Dataset> datasetPage = this.page(page, wrapper);

        // 转换为DatasetVO分页结果
        Page<DatasetVO> voPage = new Page<>(datasetPage.getCurrent(), datasetPage.getSize(), datasetPage.getTotal());
        voPage.setRecords(datasetPage.getRecords().stream()
                .map(dataset -> dataset.asViewObject(DatasetVO.class))
                .toList());

        return voPage;
    }

    /**
     * 查询指定数据集名字的数据集是否已经存在
     *
     * @param dataName 数据集名字
     * @return 是否存在
     */
    private boolean existsDatasetByDataName(String dataName) {
        return this.query()
                .eq("data_name", dataName)
                .exists();
    }

    /**
     * 查询指定数据集名字是否已被其他数据集（排除指定 id）使用
     *
     * @param dataName  数据集名字
     * @param excludeId 要排除的数据集 id
     * @return 是否存在
     */
    private boolean existsDatasetByDataNameExcludeId(String dataName, Long excludeId) {
        return this.query()
                .eq("data_name", dataName)
                .ne("id", excludeId)
                .exists();
    }
}
