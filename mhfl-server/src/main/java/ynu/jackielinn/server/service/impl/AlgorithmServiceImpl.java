package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.dto.request.ListAlgorithmRO;
import ynu.jackielinn.server.dto.response.AlgorithmVO;
import ynu.jackielinn.server.entity.Algorithm;
import ynu.jackielinn.server.mapper.AlgorithmMapper;
import ynu.jackielinn.server.service.AlgorithmService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlgorithmServiceImpl extends ServiceImpl<AlgorithmMapper, Algorithm> implements AlgorithmService {

    /**
     * 管理员创建算法，需要检查算法名字是否存在重名
     *
     * @param algorithmName 算法名字
     * @return null 表示成功，否则为错误信息
     */
    @Override
    public String createAlgorithm(String algorithmName) {
        if (this.existsAlgorithmByAlgorithmName(algorithmName)) {
            return "此算法名字已存在，请更换一个新的名字";
        }
        Algorithm algorithm = Algorithm.builder()
                .algorithmName(algorithmName)
                .build();
        if (this.save(algorithm)) {
            return null;
        } else {
            return "内部错误，请联系管理员";
        }
    }

    /**
     * 管理员逻辑删除算法（手动设置 is_deleted = 1 和 delete_time）
     * 由于 deleted 字段使用了 @TableLogic 注解，updateById 不会更新该字段
     * 因此使用 LambdaUpdateWrapper 显式更新 is_deleted 和 delete_time
     * update_time 会通过 updateFill 自动更新
     *
     * @param id 要删除的算法 id
     * @return null 表示成功，否则为错误信息
     */
    @Override
    public String deleteAlgorithm(Long id) {
        Algorithm algorithm = this.getById(id);
        if (algorithm == null) {
            return "算法不存在";
        }
        // 使用 LambdaUpdateWrapper 显式更新 is_deleted 和 delete_time
        // 由于 deleted 字段使用了 @TableLogic，updateById 不会更新该字段
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Algorithm> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Algorithm::getId, id)
                .set(Algorithm::getDeleted, 1)
                .set(Algorithm::getDeleteTime, now);
        if (this.update(updateWrapper)) {
            return null;
        }
        return "删除失败，请联系管理员";
    }

    /**
     * 管理员更新算法名字，需要检查算法名字是否已被其他算法使用（排除当前算法）
     * 使用 updateById 会触发 updateFill，自动更新 update_time
     *
     * @param id            要更新的算法 id
     * @param algorithmName 新的算法名字
     * @return null 表示成功，否则为错误信息
     */
    @Override
    public String updateAlgorithm(Long id, String algorithmName) {
        Algorithm algorithm = this.getById(id);
        if (algorithm == null) {
            return "算法不存在";
        }

        // 检查算法名字唯一性（排除当前算法）
        if (existsAlgorithmByAlgorithmNameExcludeId(algorithmName, id)) {
            return "此算法名字已被其他算法使用，请更换一个新的名字";
        }

        // 使用 updateById 更新算法名字，会自动触发 updateFill，更新 update_time
        Algorithm updateAlgorithm = Algorithm.builder()
                .id(id)
                .algorithmName(algorithmName)
                .build();
        if (this.updateById(updateAlgorithm)) {
            return null;
        } else {
            return "更新失败，请联系管理员";
        }
    }

    /**
     * 管理员查询算法列表（支持关键字模糊查询和时间范围查询，分页或不分页）
     * 关键字为空则查询全部，不为空则在算法名字中模糊查询
     * 当 all=true 时，返回全部结果（不分页）；否则返回分页结果
     *
     * @param ro 查询条件对象
     * @return 分页结果（AlgorithmVO，排除敏感信息）
     */
    @Override
    public IPage<AlgorithmVO> listAlgorithms(ListAlgorithmRO ro) {
        // 处理默认值（@ModelAttribute 绑定可能不会应用 @Builder.Default）
        boolean all = ro.getAll() != null && ro.getAll();
        LambdaQueryWrapper<Algorithm> wrapper = new LambdaQueryWrapper<>();

        // 关键字模糊查询（算法名字）
        if (ro.getKeyword() != null && !ro.getKeyword().trim().isEmpty()) {
            String keywordTrimmed = ro.getKeyword().trim();
            wrapper.like(Algorithm::getAlgorithmName, keywordTrimmed);
        }

        // 创建时间范围查询（将 LocalDate 转换为 LocalDateTime）
        LocalDate startDate = ro.getStartTime();
        LocalDate endDate = ro.getEndTime();
        if (startDate != null && endDate != null) {
            // 两个时间都有：时间范围内（起始时间当天 00:00:00 到终止时间当天 23:59:59）
            LocalDateTime startTime = startDate.atStartOfDay();
            LocalDateTime endTime = endDate.atTime(23, 59, 59);
            wrapper.between(Algorithm::getCreateTime, startTime, endTime);
        } else if (startDate != null) {
            // 起始时间有，终止时间空：从起始时间当天 00:00:00 到现在
            LocalDateTime startTime = startDate.atStartOfDay();
            wrapper.ge(Algorithm::getCreateTime, startTime);
        } else if (endDate != null) {
            // 起始时间空，终止时间有：终止时间当天 23:59:59 之前
            LocalDateTime endTime = endDate.atTime(23, 59, 59);
            wrapper.le(Algorithm::getCreateTime, endTime);
        }
        // 两个时间都空：不限时间（不添加条件）

        // 按 id 从小到大排序
        wrapper.orderByAsc(Algorithm::getId);

        IPage<Algorithm> algorithmPage;
        if (all) {
            // 不分页：查询所有结果
            List<Algorithm> allAlgorithms = this.list(wrapper);
            // 创建一个分页对象，但包含所有数据
            Page<Algorithm> allPage = new Page<>();
            allPage.setRecords(allAlgorithms);
            allPage.setTotal(allAlgorithms.size());
            allPage.setCurrent(1);
            allPage.setSize(allAlgorithms.size());
            algorithmPage = allPage;
        } else {
            // 分页查询
            long current = ro.getCurrent() != null ? ro.getCurrent() : 1L;
            long size = ro.getSize() != null ? ro.getSize() : 10L;
            Page<Algorithm> page = new Page<>(current, size);
            algorithmPage = this.page(page, wrapper);
        }

        // 转换为AlgorithmVO分页结果
        Page<AlgorithmVO> voPage = new Page<>(algorithmPage.getCurrent(), algorithmPage.getSize(), algorithmPage.getTotal());
        voPage.setRecords(algorithmPage.getRecords().stream()
                .map(algorithm -> algorithm.asViewObject(AlgorithmVO.class))
                .toList());

        return voPage;
    }

    /**
     * 查询指定算法名字的算法是否已经存在
     *
     * @param algorithmName 算法名字
     * @return 是否存在
     */
    private boolean existsAlgorithmByAlgorithmName(String algorithmName) {
        return this.query()
                .eq("algorithm_name", algorithmName)
                .exists();
    }

    /**
     * 查询指定算法名字是否已被其他算法（排除指定 id）使用
     *
     * @param algorithmName 算法名字
     * @param excludeId     要排除的算法 id
     * @return 是否存在
     */
    private boolean existsAlgorithmByAlgorithmNameExcludeId(String algorithmName, Long excludeId) {
        return this.query()
                .eq("algorithm_name", algorithmName)
                .ne("id", excludeId)
                .exists();
    }
}
