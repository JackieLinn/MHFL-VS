package ynu.jackielinn.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ynu.jackielinn.server.entity.Client;

import java.util.List;

public interface ClientService extends IService<Client> {

    void saveClient(Client client);

    /**
     * 在给定 round id 集合中，查询指定 client_index 的最新一条 Client（按 timestamp 降序）
     */
    Client getLatestByRidsAndClientIndex(List<Long> rids, Integer clientIndex);

    /**
     * 在给定 round id 集合中，查询指定 client_index 的全部 Client，按 timestamp 升序（用于客户端详情）
     */
    List<Client> listByRidsAndClientIndex(List<Long> rids, Integer clientIndex);

    /**
     * 在给定 round id 集合中，查询全部 Client（用于复制任务时拷贝 Client 数据）
     */
    List<Client> listByRidIn(List<Long> rids);
}
