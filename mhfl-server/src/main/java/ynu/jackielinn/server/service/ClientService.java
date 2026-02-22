package ynu.jackielinn.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ynu.jackielinn.server.entity.Client;

public interface ClientService extends IService<Client> {

    void saveClient(Client client);
}
