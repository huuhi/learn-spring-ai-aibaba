package alibaba.datafilter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import alibaba.datafilter.model.domain.Collection;
import alibaba.datafilter.mapper.CollectionMapper;
import alibaba.datafilter.service.CollectionService;
import org.springframework.stereotype.Service;

import static alibaba.datafilter.common.content.RedisConstant.TEMP_USER_ID;

/**
* @author windows
* @description 针对表【collection】的数据库操作Service实现
* @createDate 2025-09-26 14:19:18
*/
@Service
public class CollectionServiceImpl extends ServiceImpl<CollectionMapper, Collection>
    implements CollectionService {
    public Boolean isContains(String collectionName) {
//        TODO 之后需要修改用户ID
        return lambdaQuery()
                .eq(Collection::getCollectionName, collectionName)
                .eq(Collection::getUserId,TEMP_USER_ID).count() > 0;
    }

}




