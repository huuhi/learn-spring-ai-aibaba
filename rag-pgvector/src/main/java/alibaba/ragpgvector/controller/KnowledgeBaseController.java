package alibaba.ragpgvector.controller;

import alibaba.ragpgvector.service.impl.KnowledgeBaseServiceImpl;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;


/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/21
 * 说明: 实现保存知识库，检索知识库，问答
 */
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeBaseController {
    @Resource
    private  KnowledgeBaseServiceImpl knowledgeBaseService;


    /**
     *
     * @param content 插入的文本内容
     * @return 响应体
     */
    @GetMapping("/insert-text")
    public ResponseEntity<String> insertText(@RequestParam("content") String content) {
        if(content==null||content.trim().isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("文本内容无效");
        }
        try {
            knowledgeBaseService.insertText(content);
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
    public ResponseEntity<String> uploadFile(@RequestParam("files") MultipartFile[] files) {
//
        if(files==null||files.length==0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("上传文件为空！");
        }
        try {
            String result = knowledgeBaseService.loadFileByType(files);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件上传失败: " + e.getMessage());
        }
    }


    @GetMapping("/search")
    public ResponseEntity<?> similaritySearch(@RequestParam("query") String query,
                                              @RequestParam(value = "topK",defaultValue = "5")int topK){
        if(query==null||query.trim().isEmpty()){
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("查询内容是必需的");
        }
        if(topK<=0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("topK必须大于0");
        }
        try {
            List<Document> result = knowledgeBaseService.searchSimilar(query, topK);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("相似性搜索过程中发生错误: " + e.getMessage());
        }
    }
    @GetMapping(value = "/chat")
    public ResponseEntity<?> chatWithKnowledge(@RequestParam("query") String query,
                                                     @RequestParam(value = "topK",defaultValue = "5")int topK){
        if(query==null||query.trim().isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("查询内容是必需的");
        }
        if(topK<=0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("topK必须大于0");
        }
        try {
            String result = knowledgeBaseService.chatWithKnowledge(query, topK);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("流式对话过程中发生错误: " + e.getMessage());

        }
    }
    @GetMapping(value = "/stream-chat",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> chatWithKnowledgeStream(@RequestParam("query") String query,
                                              @RequestParam(value = "topK",defaultValue = "5")int topK){
        if(query==null||query.trim().isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("查询内容是必需的");
        }
        if(topK<=0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("topK必须大于0");
        }
        try {
            Flux<String> result = knowledgeBaseService.streamChatWithKnowledge(query, topK);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Flux.just("流式对话过程中发生错误: " + e.getMessage()));

        }
    }

}
