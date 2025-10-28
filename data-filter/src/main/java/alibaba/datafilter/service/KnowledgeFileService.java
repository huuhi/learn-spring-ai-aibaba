package alibaba.datafilter.service;

import alibaba.datafilter.model.domain.KnowledgeFile;
import alibaba.datafilter.model.vo.FileVo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author windows
* @description 针对表【knowledge_file(知识库文件信息表)】的数据库操作Service
* @createDate 2025-10-18 17:14:21
*/
@Service
public interface KnowledgeFileService extends IService<KnowledgeFile> {

    List<Long> uploadFile(MultipartFile[] file);

    List<FileVo> getFileList();

    List<FileVo> getFileListByIds(List<Long> ids);

    void deleteFiles(Long[] ids);
}
