package ynu.jackielinn.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ynu.jackielinn.server.entity.Round;

import java.util.List;

public interface RoundService extends IService<Round> {

    /**
     * 保存单条轮次记录（由训练消息处理器写库时调用）。
     *
     * @param round 轮次记录实体
     */
    void saveRound(Round round);

    /**
     * 根据任务 id 与轮次号查询唯一 Round（用于 Client 写入时关联 rid）。
     *
     * @param tid      任务 id
     * @param roundNum 轮次编号
     * @return 唯一 Round，不存在返回 null
     */
    Round getByTidAndRoundNum(Long tid, Integer roundNum);

    /**
     * 根据任务 id 查询轮次列表，按 roundNum 升序（用于历史曲线）。
     *
     * @param tid 任务 id
     * @return 该任务下全部 Round，按 roundNum 升序
     */
    List<Round> listByTidOrderByRoundNum(Long tid);
}
