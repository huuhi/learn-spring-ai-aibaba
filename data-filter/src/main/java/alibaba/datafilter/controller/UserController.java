package alibaba.datafilter.controller;

import alibaba.datafilter.common.utils.EmailUtils;
import alibaba.datafilter.service.UserService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/26
 * 说明:
 */

@RestController
@Slf4j
@RequiredArgsConstructor
public class UserController {
//    TODO:暂时不写拦截器，之后再写
    private  final UserService userService;
    private final EmailUtils emailUtils;
    @PostMapping("/register")
    public ResponseEntity<String> login(@RequestParam String email, @RequestParam String authCode) {
//        TODO：需要用到redis，去查看缓存里的验证码
        Boolean correct = userService.verifyCode(email, authCode);
        if (correct) {
            return ResponseEntity.ok("注册成功");
        }
        return ResponseEntity.badRequest().body("验证码错误");
    }

//    发送邮箱验证码
    @PostMapping("/sendEmail")
    public String sendEmail(@RequestParam String email) {
//        TODO:需要用到redis，生成验证码，并保存到缓存里
        try {

            emailUtils.sendEmail(email,"登录验证码");
        } catch (MessagingException e) {
            log.error("发送邮件失败:{}",e.getMessage());
            throw new RuntimeException(e);
        }
        return "发送成功";
    }

}
