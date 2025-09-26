package alibaba.datafilter.service.impl;

import alibaba.datafilter.common.content.RedisConstant;
import alibaba.datafilter.common.utils.RedisUtils;
import cn.hutool.captcha.generator.RandomGenerator;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import alibaba.datafilter.model.domain.User;
import alibaba.datafilter.service.UserService;
import alibaba.datafilter.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author windows
* @description 针对表【user】的数据库操作Service实现
* @createDate 2025-09-26 15:02:56
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{
    private final RedisUtils redisUtils;

    public UserServiceImpl(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    @Override
    public Boolean verifyCode(String email, String authCode) {
        Boolean exists = redisUtils.exists(RedisConstant.EMAIL_CODE_PREFIX + email);
//        TODO 还需要判断用户是否存在,如果不存在则注册
//        如果存在,判断验证码是否一致
        if (exists && redisUtils.get(RedisConstant.EMAIL_CODE_PREFIX + email).equals(authCode)) {
            //判断邮箱是否已注册
            if( query().eq("email", email).count()==0){
                register(email);
            }
            return true;
        }
        return false;
    }
    private void register(String email) {
//        如果没有指定用户名就随机生成一个 随机存一个用户头像
        String name = new RandomGenerator("小小怪", 6).generate();
        User user = User.builder().email(email).name(name).build();
        save(user);
    }
}




