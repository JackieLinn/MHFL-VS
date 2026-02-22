package ynu.jackielinn.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ynu.jackielinn.server.entity.Round;

public interface RoundService extends IService<Round> {

    void saveRound(Round round);

    /**
     * 根据任务 id 与轮次号查询唯一 Round（用于 Client 写入时关联 rid）
     */
    Round getByTidAndRoundNum(Long tid, Integer roundNum);
}
