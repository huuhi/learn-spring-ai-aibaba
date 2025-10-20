package alibaba.datafilter.tools;


import alibaba.datafilter.model.domain.Collection;
import alibaba.datafilter.model.dto.RagSearchConfigDTO;
import alibaba.datafilter.service.CollectionService;
import alibaba.datafilter.service.UserService;
import alibaba.datafilter.utils.RagUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/20
 * 说明: 知识库工具
 */
@Slf4j
@Component
public class RagTool {
    @Resource
    private  CollectionService collectionService;
    @Resource
    private  UserService userService;
    @Resource
    private  RagUtils ragUtils;



    @Tool(name="get_collection_list",description = "根据用户ID获取知识库基本信息")
    public String getCollectionList(@ToolParam(description = "用户的id") Integer userId){
        if (userId==null||userService.getById(userId)==null){
            return "用户ID为空或用户不存在！";
        }
        List<Collection> list = collectionService.lambdaQuery()
                .eq(Collection::getUserId, userId)
                .list();
        return list.stream().map(collection -> "知识库名称："+collection.getCollectionName() + "知识库介绍:" + collection.getDescription()+"知识库语言:"+collection.getLanguage()).toList().toString();
    }


    @Tool(name = "rag_search", description = "按照传递的问题和参数执行知识库检索")
    public String ragSearch(@ToolParam(description = "知识库名称") String collectionName,
                            @ToolParam(description = "用户的问题，最好与知识库的语言一样") String query,
                            @ToolParam(description = "知识库检索参数" ,required = false) RagSearchConfigDTO ragSearchConfigDTO) {

        return ragUtils.ragSearch(query,collectionName,ragSearchConfigDTO);
    }
}