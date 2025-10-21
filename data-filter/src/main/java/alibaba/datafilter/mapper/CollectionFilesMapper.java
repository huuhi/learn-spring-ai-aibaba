package alibaba.datafilter.mapper;


import alibaba.datafilter.model.domain.CollectionFiles;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;

/**
* @author windows
* @description 针对表【collection_files】的数据库操作Mapper
* @createDate 2025-10-19 14:01:08
* @Entity alibaba.datafilter.model.domain.CollectionFiles
*/
public interface CollectionFilesMapper extends BaseMapper<CollectionFiles> {

    @Insert("insert into collection_files (file_id, collection_id, status) values (#{fileId}, #{collectionId}, CAST(#{status} AS status))")
    int saveAutoStatus(CollectionFiles collectionFiles);

}




