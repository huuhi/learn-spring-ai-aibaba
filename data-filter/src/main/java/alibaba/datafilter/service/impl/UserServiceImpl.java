package alibaba.datafilter.service.impl;

import alibaba.datafilter.common.content.AvatarContent;
import alibaba.datafilter.common.content.RedisConstant;
import alibaba.datafilter.common.utils.RedisUtils;
import alibaba.datafilter.exception.ValidationException;
import alibaba.datafilter.model.dto.LoginDTO;
import alibaba.datafilter.model.dto.UserDTO;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import alibaba.datafilter.model.domain.User;
import alibaba.datafilter.service.UserService;
import alibaba.datafilter.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

import static alibaba.datafilter.common.content.RedisConstant.USER_DATA_PREFIX;

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
    public String login(LoginDTO loginDTO) {
        String email = loginDTO.getEmail();
        String authCode = loginDTO.getAuthCode();
        User user;
//        先判断是密码登录还是验证码登录
        if (loginDTO.getType().equals("password")) {
            user= loginByPassword(email, loginDTO.getPassword());
        }else{
            user= loginByCode(email, authCode);
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return generateToken(userDTO);
    }

    private User loginByCode(String email, String authCode) {
//        判断验证码是否存在
        Boolean exists = redisUtils.exists(RedisConstant.EMAIL_CODE_PREFIX + email);
//        如果存在,判断验证码是否一致
        if (exists && redisUtils.get(RedisConstant.EMAIL_CODE_PREFIX + email).equals(authCode)) {
            //判断邮箱是否已注册
            User user = isEmailExists(email);
            if(user==null){
                register(email);
            }
            //查询

//            将验证码删除
            redisUtils.delete(RedisConstant.EMAIL_CODE_PREFIX + email);
            return user;
        }
        throw new ValidationException("验证码错误或已过期!");
    }

    private User loginByPassword(String email, String password) {
        if(isEmailExists(email)==null){
            throw new ValidationException("用户不存在！");
        }
//        获取密码
        User user = query().eq("email", email).one();
        String dbPassword =  user.getPassword();
        if(dbPassword==null||dbPassword.isEmpty()){
            throw new ValidationException("用户不存在！");
        }
//        判断密码是否正确
        if (!encoder.matches(password,dbPassword)) {
            throw new ValidationException("密码错误!");
        }
        return user;
    }


    private void register(String email) {
//        如果没有指定用户名就随机生成一个 随机存一个用户头像
        String avatar = AvatarContent.AVATAR_URL.get(random.nextInt(AvatarContent.AVATAR_URL.size()));
        String name =randomGenerator.generate();
        User user = User.builder().email(email).name("小小怪"+name).avatar(avatar).build();
        save(user);
    }

    @Override
    public void setPassword(String email, String password) {
//        TODO 需要判断当前用户是否登录，并且密码为null，否则拒绝
        if(isEmailExists(email)==null){
            throw new ValidationException("邮箱未注册！");
        }
        if (query().eq("email", email).one().getPassword()!=null) {
            throw new ValidationException("用户已设置密码！");
        }
        update().set("password", encoder.encode(password)).eq("email", email).update();
    }

    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
        if(isEmailExists(email)==null){
            throw new ValidationException("邮箱未注册！");
        }
        String password = query().eq("email", email).one().getPassword();
        if (password==null) {
            throw new ValidationException("用户未设置密码！");
        }
        if (!encoder.matches(oldPassword,password)) {
            throw new ValidationException("旧密码错误！");
        }
        if(oldPassword.equals(newPassword)){
            throw new ValidationException("新密码不能与旧密码相同！");
        }
        update().set("password", encoder.encode(newPassword)).eq("email", email).update();
    }
    private User isEmailExists(String email) {
        return query().eq("email", email).one();
    }
//    生成一个随机token
    private String generateToken(UserDTO userDTO) {
        String token = RandomUtil.randomString(30);
        redisUtils.set(USER_DATA_PREFIX+token,JSONUtil.toJsonStr(userDTO),10080L);
        return token;
    }


}