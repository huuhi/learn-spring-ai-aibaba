package alibaba.datafilter.controller;

import alibaba.datafilter.model.dto.CreateCollectionDTO;
import alibaba.datafilter.model.dto.UploadFileConfigDTO;
import alibaba.datafilter.service.KnowledgeBaseService;
import alibaba.datafilter.service.KnowledgeFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;


/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/21
 * 说明: 实现保存知识库，检索知识库，问答
 */
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeFileService knowledgeFileService;
    /**
     *
     * @param content 插入的文本内容
     *
     * @return 响应体
     */
    @GetMapping("/insert-text")
    public ResponseEntity<String> insertText(@RequestParam("content") String content,
                                             @RequestParam String collectionName) {
        if(content==null||content.trim().isEmpty()||collectionName==null||collectionName.trim().isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("文本内容无效或者知识库为空");
        }

        Boolean success = knowledgeBaseService.insertText(content, collectionName);
        if(success){
            return ResponseEntity.ok("插入成功");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("插入文本内容失败");
    }

    /**
     * @param uploadFileConfig 上传知识库配置，包括文件id等
     * @return 响应体
     */
//    TODO 这个接口需要修改成，将文件提交到知识库
    @PostMapping("/collection-knowledge")
    public ResponseEntity<String> FilesToCollection(@RequestBody @Valid UploadFileConfigDTO uploadFileConfig) {
        try {
//            TODO 这里应该修改为消息队列，或者异步处理
            String result = knowledgeBaseService.importFilesToCollection(uploadFileConfig);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件上传失败: " + e.getMessage());
        }
    }


    @GetMapping("/search")
    public ResponseEntity<?> similaritySearch(@RequestParam("query") String query,
                                              @RequestParam(value = "topK",defaultValue = "5")int topK,
                                              @RequestParam String collectionName){
        if(query==null||query.trim().isEmpty()){
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("查询内容是必需的");
        }
        if(topK<=0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("topK必须大于0");
        }
        try {
            List<Document> result = knowledgeBaseService.searchSimilar(query, topK,collectionName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("相似性搜索过程中发生错误: " + e.getMessage());
        }
    }

    /**
     *
     * @param createCollectionDTO 创建知识库的参数
     * @return 返回
     */
    @PostMapping("/createCollection")
    public ResponseEntity<String> createCollection(@RequestBody @Valid CreateCollectionDTO createCollectionDTO){
        return knowledgeBaseService.createCollection(createCollectionDTO);
    }

    /**
     *
     * @param files 上传的文件列表
     * @return 响应体,失败返回失败信息，成功返回文件id列表
     */
    @PostMapping("upload-file")
    public ResponseEntity<?> uploadFile(@RequestParam("files") MultipartFile[] files){
        if(files==null||files.length==0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("上传文件为空！");
        }
        return knowledgeFileService.uploadFile(files);
    }
//    获取所有文件列表
    @GetMapping("get-file-list")
    public ResponseEntity<?> getFileList(){
        return knowledgeFileService.getFileList();
    }


}
