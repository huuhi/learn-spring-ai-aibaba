package alibaba.datafilter.controller;

import alibaba.datafilter.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
        try {
            knowledgeBaseService.insertText(content,collectionName);
            return ResponseEntity.ok("插入成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("插入文本内容失败:"+e.getMessage());
        }
    }

    /**
     *  支持的文件类型：PDF，Word，Txt，Text，Markdown等文本类型
     * @param files 上传的文件,支持上传多个
     * @return 响应体
     */
    @PostMapping("/upload-file")
    public ResponseEntity<String> uploadFile(@RequestParam("files") MultipartFile[] files,
                                             @RequestParam String collectionName) {
//
        if(files==null||files.length==0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("上传文件为空！");
        }
        try {
            String result = knowledgeBaseService.loadFileByType(files,collectionName);
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
    @PostMapping("/createCollection")
    public ResponseEntity<String> createCollection(@RequestParam String collectionName,
                                                   @RequestParam(value = "description",required = false) String description){
        if(collectionName==null||collectionName.trim().isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("集合名称无效");
        }
        try {
            knowledgeBaseService.createCollection(collectionName, description);
            return ResponseEntity.ok("集合创建成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("集合创建失败: " + e.getMessage());
        }
    }

}
