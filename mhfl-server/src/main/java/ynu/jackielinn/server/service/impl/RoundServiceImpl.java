package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.entity.Round;

import java.util.List;

import ynu.jackielinn.server.mapper.RoundMapper;
import ynu.jackielinn.server.service.RoundService;

@Service
public class RoundServiceImpl extends ServiceImpl<RoundMapper, Round> implements RoundService {

    /**
     * 保存单条轮次记录。
     *
     * @param round 轮次记录实体
     */
    @Override
    public void saveRound(Round round) {
        save(round);
    }

    /**
     * 根据任务 id 与轮次号查询唯一 Round。
     *
     * @param tid      任务 id
     * @param roundNum 轮次编号
     * @return 唯一 Round，不存在返回 null
     */
    @Override
    public Round getByTidAndRoundNum(Long tid, Integer roundNum) {
        return lambdaQuery()
                .eq(Round::getTid, tid)
                .eq(Round::getRoundNum, roundNum)
                .last("limit 1")
                .one();
    }

    /**
     * 根据任务 id 查询轮次列表，按 roundNum 升序。
     *
     * @param tid 任务 id
     * @return 该任务下全部 Round，按 roundNum 升序
     */
    @Override
    public List<Round> listByTidOrderByRoundNum(Long tid) {
        return lambdaQuery()
                .eq(Round::getTid, tid)
                .orderByAsc(Round::getRoundNum)
                .list();
    }
}
