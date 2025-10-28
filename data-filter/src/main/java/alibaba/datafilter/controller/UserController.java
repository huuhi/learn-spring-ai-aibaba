package alibaba.datafilter.controller;

import alibaba.datafilter.common.utils.EmailUtils;
import alibaba.datafilter.model.dto.LoginDTO;
import alibaba.datafilter.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

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
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO loginDTO) {
//        需要用到redis，去查看缓存里的验证码  已完成
        String token = userService.login(loginDTO);
        return ResponseEntity.ok(token);
    }

//    发送邮箱验证码
    @PostMapping("/sendEmail")
    public ResponseEntity<String> sendEmail(@RequestParam String email) {
//        需要用到redis，生成验证码，并保存到缓存里   已完成
        Boolean success = emailUtils.sendEmail(email, "登录验证码");
        if (success){
            return ResponseEntity.ok("发送成功");
        }
        return ResponseEntity.badRequest().body("发送邮件失败");
    }
//    设置密码
    @PutMapping("/setPassword")
    public ResponseEntity<String> setPassword(@RequestParam String email, @RequestParam String password) {
        userService.setPassword(email, password);
        return ResponseEntity.status(HttpStatus.CREATED).body("设置密码成功");
    }
//    修改密码
    @PutMapping("/changePassword")
    public ResponseEntity<String> changePassword(@RequestParam String email, @RequestParam String oldPassword, @RequestParam String newPassword) {
        userService.changePassword(email, oldPassword, newPassword);
        return ResponseEntity.status(HttpStatus.CREATED).body("修改密码成功");
    }
}
