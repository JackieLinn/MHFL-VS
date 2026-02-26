package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.entity.Client;
import ynu.jackielinn.server.mapper.ClientMapper;
import ynu.jackielinn.server.service.ClientService;

import java.util.List;

@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements ClientService {

    @Override
    public void saveClient(Client client) {
        save(client);
    }

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
}
