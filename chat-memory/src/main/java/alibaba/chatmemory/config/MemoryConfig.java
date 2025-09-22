package alibaba.chatmemory.config;


import com.alibaba.cloud.ai.memory.jdbc.SQLiteChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;


/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/21
 * 说明:
 */
@Configuration
public class MemoryConfig {
    @Bean
    public SQLiteChatMemoryRepository sqliteChatMemoryRepository() {
        // 确保目录存在

        
        // 设置 SQLite 数据库文件路径

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:chat-memory/src/main/resources/chat-memory.db");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        return SQLiteChatMemoryRepository.sqliteBuilder()
                .jdbcTemplate(jdbcTemplate)
                .build();
    }
}
