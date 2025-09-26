package alibaba.datafilter.service;

import alibaba.datafilter.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
* @author windows
* @description 针对表【user】的数据库操作Service
* @createDate 2025-09-26 15:02:56
*/
@Service
public interface UserService extends IService<User> {

    Boolean verifyCode(String email, String authCode);
}
