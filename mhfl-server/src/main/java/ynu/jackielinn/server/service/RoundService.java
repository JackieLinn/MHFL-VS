package ynu.jackielinn.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ynu.jackielinn.server.entity.Round;

public interface RoundService extends IService<Round> {

    void saveRound(Round round);
}
