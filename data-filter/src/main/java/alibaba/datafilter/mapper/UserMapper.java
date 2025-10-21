package alibaba.datafilter.mapper;

import alibaba.datafilter.model.domain.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author windows
* @description 针对表【user】的数据库操作Mapper
* @createDate 2025-09-26 15:02:56
* @Entity alibaba.datafilter.model.domain.User
*/
public interface UserMapper extends BaseMapper<User> {

    User findByUsername(String username);
}




