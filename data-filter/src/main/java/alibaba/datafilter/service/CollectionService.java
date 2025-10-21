package alibaba.datafilter.service;

import alibaba.datafilter.model.domain.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

/**
* @author windows
* @description 针对表【collection】的数据库操作Service
* @createDate 2025-09-26 14:19:18
*/
@Service
public interface CollectionService extends IService<Collection> {
    Collection isContains(String collectionName);
}
