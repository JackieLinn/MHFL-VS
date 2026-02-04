package ynu.jackielinn.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import ynu.jackielinn.server.entity.Task;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
