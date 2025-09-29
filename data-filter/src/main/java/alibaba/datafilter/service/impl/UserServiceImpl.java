package alibaba.datafilter.service.impl;

import alibaba.datafilter.common.content.AvatarContent;
import alibaba.datafilter.common.content.RedisConstant;
import alibaba.datafilter.common.utils.RedisUtils;
import alibaba.datafilter.model.dto.LoginDTO;
import cn.hutool.captcha.generator.RandomGenerator;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import alibaba.datafilter.model.domain.User;
import alibaba.datafilter.service.UserService;
import alibaba.datafilter.mapper.UserMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
* @author windows
* @description 针对表【user】的数据库操作Service实现
* @createDate 2025-09-26 15:02:56
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{
//    加密
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final RedisUtils redisUtils;
    private final RandomGenerator randomGenerator = new RandomGenerator(6);
    private final Random random = new Random();

    public UserServiceImpl(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    @Override
    public ResponseEntity<String> login(LoginDTO loginDTO) {
        String email = loginDTO.getEmail();
        String authCode = loginDTO.getAuthCode();
//        先判断是密码登录还是验证码登录
        if (loginDTO.getType().equals("password")) {
            return  loginByPassword(email, loginDTO.getPassword());
        }
        return loginByCode(email, authCode);
    }

    private ResponseEntity<String> loginByCode(String email, String authCode) {
//        判断验证码是否存在
        Boolean exists = redisUtils.exists(RedisConstant.EMAIL_CODE_PREFIX + email);
//        如果存在,判断验证码是否一致
        if (exists && redisUtils.get(RedisConstant.EMAIL_CODE_PREFIX + email).equals(authCode)) {
            //判断邮箱是否已注册
            if(!isEmailExists(email)){
                register(email);
            }
//            将验证码删除
            redisUtils.delete(RedisConstant.EMAIL_CODE_PREFIX + email);
            return  ResponseEntity.ok("登录成功");
        }
        return  ResponseEntity.badRequest().body("验证码错误或已过期！");
    }

    private ResponseEntity<String> loginByPassword(String email, String password) {
        if(!isEmailExists(email)){
            return  ResponseEntity.badRequest().body("邮箱未注册！");
        }
//        获取密码
        String dbPassword = query().eq("email", email).one().getPassword();
        if(dbPassword==null||dbPassword.isEmpty()){
            return  ResponseEntity.badRequest().body("用户不存在！");
        }
//        判断密码是否正确
        if (encoder.matches(password,dbPassword)) {
            return ResponseEntity.ok("登录成功");
        }
        return ResponseEntity.badRequest().body("密码错误！");
    }


    private void register(String email) {
//        如果没有指定用户名就随机生成一个 随机存一个用户头像
        String avatar = AvatarContent.AVATAR_URL.get(random.nextInt(AvatarContent.AVATAR_URL.size()));
        String name =randomGenerator.generate();
        User user = User.builder().email(email).name("小小怪"+name).avatar(avatar).build();
        save(user);
    }

    @Override
    public ResponseEntity<String> setPassword(String email, String password) {
//        TODO 需要判断当前用户是否登录，并且密码为null，否则拒绝
        if(!isEmailExists(email)){
            return  ResponseEntity.badRequest().body("邮箱未注册！");
        }
        if (query().eq("email", email).one().getPassword()!=null) {
            return  ResponseEntity.badRequest().body("用户已设置密码！");
        }
        update().set("password", encoder.encode(password)).eq("email", email).update();

        return ResponseEntity.ok("设置密码成功");
    }

    @Override
    public ResponseEntity<String> changePassword(String email, String oldPassword, String newPassword) {
        if(!isEmailExists(email)){
            return  ResponseEntity.badRequest().body("邮箱未注册！");
        }
        String password = query().eq("email", email).one().getPassword();
        if (password==null) {
            return  ResponseEntity.badRequest().body("用户未设置密码！");
        }
        if (!encoder.matches(oldPassword,password)) {
            return  ResponseEntity.badRequest().body("旧密码错误！");
        }
        if(oldPassword.equals(newPassword)){
            return  ResponseEntity.badRequest().body("新密码不能与旧密码相同！");
        }
        update().set("password", encoder.encode(newPassword)).eq("email", email).update();
        return ResponseEntity.ok("修改密码成功");
    }
    private Boolean isEmailExists(String email) {
        return query().eq("email", email).count() > 0;
    }

}