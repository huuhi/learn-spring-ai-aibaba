package alibaba.datafilter.common.utils;

import cn.hutool.captcha.generator.RandomGenerator;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import static alibaba.datafilter.common.content.RedisConstant.EMAIL_CODE_PREFIX;


@Configuration
public class EmailUtils {
    

    private final JavaMailSender mailSender;
    private final RandomGenerator randomGenerator = new RandomGenerator(6);
    private final RedisUtils redisUtils;

    public EmailUtils(JavaMailSender mailSender, RedisUtils redisUtils) {
        this.mailSender = mailSender;
        this.redisUtils = redisUtils;
    }

    public void sendEmail(String to, String subject) throws MessagingException {
        String verificationCode = randomGenerator.generate();
//         写入redis缓存！有效期5分钟！
        redisUtils.set(EMAIL_CODE_PREFIX+to,verificationCode,5L);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom("3108967414@qq.com");  // 发件人
        helper.setTo(to);                     // 收件人
        helper.setSubject(subject);           // 邮件主题
        
        // 创建HTML格式的邮件内容
        String htmlContent = buildHtmlContent(verificationCode);
        helper.setText(htmlContent, true);    // 设置为HTML格式
        
        mailSender.send(message);
    }
    
    // 构建HTML格式的邮件内容
    private String buildHtmlContent(String verificationCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>验证码邮件</title>
                <style>
                    body {
                        font-family: 'Microsoft YaHei', Arial, sans-serif;
                        background-color: #f5f5f5;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        border-radius: 10px;
                        box-shadow: 0 0 10px rgba(0,0,0,0.1);
                        overflow: hidden;
                    }
                    .header {
                        background-color: #4CAF50;
                        color: white;
                        padding: 20px;
                        text-align: center;
                    }
                    .content {
                        padding: 30px;
                        text-align: center;
                    }
                    .verification-code {
                        font-size: 32px;
                        font-weight: bold;
                        color: #4CAF50;
                        letter-spacing: 5px;
                        margin: 30px 0;
                        padding: 15px;
                        border: 2px dashed #4CAF50;
                        border-radius: 5px;
                        display: inline-block;
                    }
                    .footer {
                        background-color: #f8f8f8;
                        padding: 20px;
                        text-align: center;
                        color: #666;
                        font-size: 14px;
                    }
                    .note {
                        color: #ff5722;
                        font-size: 14px;
                        margin-top: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>欢迎使用我们的服务</h1>
                    </div>
                    <div class="content">
                        <h2>您的验证码是：</h2>
                        <div class="verification-code">%s</div>
                        <p>请在5分钟内使用此验证码完成验证。</p>
                        <p class="note">如果这不是您本人操作，请忽略此邮件。</p>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿回复。</p>
                        <p>© 2025 阿里巴巴. 保留所有权利。</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(verificationCode);
    }
}
