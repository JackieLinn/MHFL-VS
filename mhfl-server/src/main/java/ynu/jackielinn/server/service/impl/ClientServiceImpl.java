package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.entity.Client;
import ynu.jackielinn.server.mapper.ClientMapper;
import ynu.jackielinn.server.service.ClientService;

import java.util.List;

@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements ClientService {

    /**
     * 保存单条客户端训练记录。
     *
     * @param client 客户端记录实体
     */
    @Override
    public void saveClient(Client client) {
        save(client);
    }

    /**
     * 在给定 round id 集合中，查询指定 client_index 的最新一条 Client（按 timestamp 降序）。
     *
     * @param rids        round 主键 id 集合
     * @param clientIndex 客户端索引
     * @return 最新一条 Client，不存在返回 null
     */
    @Override
    public Client getLatestByRidsAndClientIndex(List<Long> rids, Integer clientIndex) {
        if (rids == null || rids.isEmpty()) {
            return null;
        }
        return lambdaQuery()
                .in(Client::getRid, rids)
                .eq(Client::getClientIndex, clientIndex)
                .orderByDesc(Client::getTimestamp)
                .last("limit 1")
                .one();
    }

    /**
     * 在给定 round id 集合中，查询指定 client_index 的全部 Client，按 timestamp 升序。
     *
     * @param rids        round 主键 id 集合
     * @param clientIndex 客户端索引
     * @return 该 client_index 下的 Client 列表
     */
    @Override
    public List<Client> listByRidsAndClientIndex(List<Long> rids, Integer clientIndex) {
        if (rids == null || rids.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(Client::getRid, rids)
                .eq(Client::getClientIndex, clientIndex)
                .orderByAsc(Client::getTimestamp)
                .list();
    }

    /**
     * 在给定 round id 集合中，查询全部 Client（用于复制任务时拷贝）。
     *
     * @param rids round 主键 id 集合
     * @return 这些 round 下的全部 Client
     */
    @Override
    public List<Client> listByRidIn(List<Long> rids) {
        if (rids == null || rids.isEmpty()) {
            return List.of();
        }
        return lambdaQuery().in(Client::getRid, rids).list();
    }
}
