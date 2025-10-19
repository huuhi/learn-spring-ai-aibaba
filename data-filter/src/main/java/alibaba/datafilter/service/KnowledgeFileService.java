package alibaba.datafilter.service;

import alibaba.datafilter.model.domain.CollectionFiles;
import alibaba.datafilter.model.domain.KnowledgeFile;
import alibaba.datafilter.model.dto.UploadFileConfigDTO;
import alibaba.datafilter.model.vo.FileVo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author windows
* @description 针对表【knowledge_file(知识库文件信息表)】的数据库操作Service
* @createDate 2025-10-18 17:14:21
*/
public interface KnowledgeFileService extends IService<KnowledgeFile> {

    ResponseEntity<?> uploadFile(MultipartFile[] file);

    ResponseEntity<List<FileVo>> getFileList();

    List<FileVo> getFileListByIds(List<Long> ids);

}
