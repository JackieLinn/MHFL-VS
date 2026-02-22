package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.entity.Client;
import ynu.jackielinn.server.mapper.ClientMapper;
import ynu.jackielinn.server.service.ClientService;

@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements ClientService {

    @Override
    public void saveClient(Client client) {
        save(client);
    }
}
