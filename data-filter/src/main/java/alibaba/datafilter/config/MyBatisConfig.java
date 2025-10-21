package alibaba.datafilter.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/2/9
 * 说明: MyBatisPlus 配置
 */
@Configuration
public class MyBatisConfig {
    // 1. 显式创建 GlobalConfig Bean
    @Bean
    public GlobalConfig globalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setIdType(IdType.AUTO); // 强制指定自增
        globalConfig.setDbConfig(dbConfig);
        return globalConfig;
    }
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL)); // 如果配置多个插件, 切记分页最后添加
        // 如果有多数据源可以不配具体类型, 否则都建议配上具体的 DbType
        return interceptor;
    }
    @Bean
    public SqlSessionFactory sqlSessionFactory(
            DataSource dataSource,
            MybatisPlusInterceptor interceptor,
            GlobalConfig globalConfig // 注入MyBatis-Plus的全局配置
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // 关键修改：继承全局配置
        MybatisConfiguration mybatisConfig = new MybatisConfiguration();
        mybatisConfig.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(mybatisConfig);
        factoryBean.setGlobalConfig(globalConfig); // 继承全局配置（包括id-type）

        factoryBean.setPlugins(interceptor);
        return factoryBean.getObject();
    }
}
