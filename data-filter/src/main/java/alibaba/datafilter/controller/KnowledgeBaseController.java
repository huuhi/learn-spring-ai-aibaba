package alibaba.datafilter.controller;

import alibaba.datafilter.common.exception.ResourceNotFoundException;
import alibaba.datafilter.model.dto.CreateCollectionDTO;
import alibaba.datafilter.model.dto.UploadFileConfigDTO;
import alibaba.datafilter.model.vo.FileVo;
import alibaba.datafilter.service.KnowledgeBaseService;
import alibaba.datafilter.service.KnowledgeFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.CrossOrigin;

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
@CrossOrigin(origins = "*")
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
        knowledgeBaseService.insertText(content, collectionName);
        return ResponseEntity.ok("插入成功");
    }

    /**
     * @param uploadFileConfig 上传知识库配置，包括文件id等
     * @return 响应体
     */
    @PostMapping("/collection-knowledge")
    public ResponseEntity<String> FilesToCollection(@RequestBody @Valid UploadFileConfigDTO uploadFileConfig) {
//            这里应该修改为消息队列，或者异步处理，已修改成异步处理
        knowledgeBaseService.importFilesToCollection(uploadFileConfig);
        return ResponseEntity.ok("导入成功,正在处理中！");
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
        knowledgeBaseService.createCollection(createCollectionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body("创建成功");
    }

    /**
     *
     * @param files 上传的文件列表
     * @return 响应体,失败返回失败信息，成功返回文件id列表
     */
    @PostMapping("upload-file")
    public ResponseEntity<List<Long>> uploadFile(@RequestParam("files") MultipartFile[] files){
        if(files==null||files.length==0){
            throw new ResourceNotFoundException("上传的文件不能为空!");
        }
        List<Long> ids = knowledgeFileService.uploadFile(files);
        return ResponseEntity.ok(ids);
    }
//    获取所有文件列表
    @GetMapping("get-file-list")
    public ResponseEntity<List<FileVo>> getFileList(){
        List<FileVo> fileList = knowledgeFileService.getFileList();
        return ResponseEntity.ok(fileList);
    }
    @DeleteMapping("delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable(name = "id") Long[] ids){
        if(ids==null||ids.length==0){
            return ResponseEntity.badRequest().body("非法文件id");
        }
        knowledgeFileService.deleteFiles(ids);
        return ResponseEntity.ok("删除成功");
    }

    @GetMapping("get-collection")
    public List<?> getCollection(){
        return knowledgeBaseService.getCollection();
    }

}
