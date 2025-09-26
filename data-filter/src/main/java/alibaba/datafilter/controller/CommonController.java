package alibaba.datafilter.controller;

import alibaba.datafilter.common.utils.AliOssUtil;
import com.aliyuncs.exceptions.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/26
 * 说明: 公共接口,上传头像
 */
@RestController("/common")
@Slf4j
public class CommonController {
    private final AliOssUtil aliOssUtil;

    public CommonController(AliOssUtil aliOssUtil) {
        this.aliOssUtil = aliOssUtil;
    }
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file){
        //        将文件转换成为字节数组
        log.info("上传文件{}",file.getOriginalFilename());
        String upload;
        try {
            upload = aliOssUtil.upload(file.getBytes(), Objects.requireNonNull(file.getOriginalFilename()));
//            生成随机文件名
        } catch (IOException e) {
            log.error("上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("上传失败");
        } catch (ClientException e) {
            log.error("上传失败", e);
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(upload);
    }
}
