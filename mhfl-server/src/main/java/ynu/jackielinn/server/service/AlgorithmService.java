package ynu.jackielinn.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import ynu.jackielinn.server.dto.request.ListAlgorithmRO;
import ynu.jackielinn.server.dto.response.AlgorithmVO;
import ynu.jackielinn.server.entity.Algorithm;

public interface AlgorithmService extends IService<Algorithm> {

    /**
     * 管理员创建算法
     *
     * @param algorithmName 算法名字
     * @return null 表示成功，否则为错误信息
     */
    String createAlgorithm(String algorithmName);

    /**
     * 管理员逻辑删除算法
     *
     * @param id 要删除的算法 id
     * @return null 表示成功，否则为错误信息
     */
    String deleteAlgorithm(Long id);

    /**
     * 管理员更新算法名字
     *
     * @param id            要更新的算法 id
     * @param algorithmName 新的算法名字
     * @return null 表示成功，否则为错误信息
     */
    String updateAlgorithm(Long id, String algorithmName);

    /**
     * 管理员查询算法列表（支持关键字模糊查询和时间范围查询，分页）
     *
     * @param ro 查询条件对象
     * @return 分页结果（AlgorithmVO，排除敏感信息）
     */
    IPage<AlgorithmVO> listAlgorithms(ListAlgorithmRO ro);
}
