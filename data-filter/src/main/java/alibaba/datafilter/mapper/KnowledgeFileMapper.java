package alibaba.datafilter.mapper;

import alibaba.datafilter.model.domain.KnowledgeFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author windows
* @description 针对表【knowledge_file(知识库文件信息表)】的数据库操作Mapper
* @createDate 2025-10-18 17:14:21
* @Entity alibaba.datafilter.model.domain.KnowledgeFile
*/
public interface KnowledgeFileMapper extends BaseMapper<KnowledgeFile> {

    int saveBatchAutoStatus(@Param("knowledgeFiles") List<KnowledgeFile> knowledgeFiles);
}




