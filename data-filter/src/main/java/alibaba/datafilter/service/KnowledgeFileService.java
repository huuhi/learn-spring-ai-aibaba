package alibaba.datafilter.service;

import alibaba.datafilter.model.domain.KnowledgeFile;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
* @author windows
* @description 针对表【knowledge_file(知识库文件信息表)】的数据库操作Service
* @createDate 2025-10-18 17:14:21
*/
public interface KnowledgeFileService extends IService<KnowledgeFile> {

    ResponseEntity<String> uploadFile(MultipartFile[] file);
}
