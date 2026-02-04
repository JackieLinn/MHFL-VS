package ynu.jackielinn.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import ynu.jackielinn.server.entity.Dataset;

@Mapper
public interface DatasetMapper extends BaseMapper<Dataset> {
}
