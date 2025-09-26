package alibaba.ragalibabademo.controller;


import alibaba.ragalibabademo.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/23
 * 说明:
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class CloudRagController {
    private final RagService ragService;


    /**
     *
     * @param files 上传文件
     * @return 是否成功
     */
    @PostMapping("/importDocument")
    public String importDocument(@RequestParam("files") MultipartFile[] files){
        return  ragService.importDocuments(files);
    }

    @GetMapping("/chat")
    public Flux<String> chat(@RequestParam String query){
        return ragService.retrieve(query).mapNotNull(x->x.getResult().getOutput().getText());
    }

}
