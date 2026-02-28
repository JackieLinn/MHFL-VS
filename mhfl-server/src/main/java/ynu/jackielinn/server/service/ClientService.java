package ynu.jackielinn.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ynu.jackielinn.server.entity.Client;

import java.util.List;

public interface ClientService extends IService<Client> {

    /**
     * 保存单条客户端训练记录（由训练消息处理器写库时调用）。
     *
     * @param client 客户端记录实体
     */
    void saveClient(Client client);

    /**
     * 在给定 round id 集合中，查询指定 client_index 的最新一条 Client（按 timestamp 降序）。
     *
     * @param rids        round 主键 id 集合
     * @param clientIndex 客户端索引
     * @return 最新一条 Client，不存在返回 null
     */
    Client getLatestByRidsAndClientIndex(List<Long> rids, Integer clientIndex);

    /**
     * 在给定 round id 集合中，查询指定 client_index 的全部 Client，按 timestamp 升序（用于客户端详情表格/曲线）。
     *
     * @param rids        round 主键 id 集合
     * @param clientIndex 客户端索引
     * @return 该 client_index 下的 Client 列表，按 roundNum 升序
     */
    List<Client> listByRidsAndClientIndex(List<Long> rids, Integer clientIndex);

    /**
     * 在给定 round id 集合中，查询全部 Client（用于复制任务时拷贝 Client 数据）。
     *
     * @param rids round 主键 id 集合
     * @return 这些 round 下的全部 Client
     */
    List<Client> listByRidIn(List<Long> rids);
}
