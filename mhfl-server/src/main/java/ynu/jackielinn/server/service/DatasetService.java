package ynu.jackielinn.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import ynu.jackielinn.server.dto.request.ListDatasetRO;
import ynu.jackielinn.server.dto.response.DatasetVO;
import ynu.jackielinn.server.entity.Dataset;

public interface DatasetService extends IService<Dataset> {

    /**
     * 管理员创建数据集
     *
     * @param dataName 数据集名字
     * @return null 表示成功，否则为错误信息
     */
    String createDataset(String dataName);

    /**
     * 管理员逻辑删除数据集
     *
     * @param id 要删除的数据集 id
     * @return null 表示成功，否则为错误信息
     */
    String deleteDataset(Long id);

    /**
     * 管理员更新数据集名字
     *
     * @param id       要更新的数据集 id
     * @param dataName 新的数据集名字
     * @return null 表示成功，否则为错误信息
     */
    String updateDataset(Long id, String dataName);

    /**
     * 管理员查询数据集列表（支持关键字模糊查询和时间范围查询，分页）
     *
     * @param ro 查询条件对象
     * @return 分页结果（DatasetVO，排除敏感信息）
     */
    IPage<DatasetVO> listDatasets(ListDatasetRO ro);
}
