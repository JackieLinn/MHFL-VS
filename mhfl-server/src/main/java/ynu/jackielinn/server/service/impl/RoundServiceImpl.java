package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.entity.Round;
import ynu.jackielinn.server.mapper.RoundMapper;
import ynu.jackielinn.server.service.RoundService;

@Service
public class RoundServiceImpl extends ServiceImpl<RoundMapper, Round> implements RoundService {

    @Override
    public void saveRound(Round round) {
        save(round);
    }
}
