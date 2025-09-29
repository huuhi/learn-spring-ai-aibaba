package alibaba.datafilter.service;

import alibaba.datafilter.model.domain.User;
import alibaba.datafilter.model.dto.LoginDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
* @author windows
* @description 针对表【user】的数据库操作Service
* @createDate 2025-09-26 15:02:56
*/
@Service
public interface UserService extends IService<User> {


    ResponseEntity<String> setPassword(String email, String password);

    ResponseEntity<String> changePassword(String email, String oldPassword, String newPassword);


    ResponseEntity<String> login(LoginDTO loginDTO);
}
