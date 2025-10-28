package alibaba.datafilter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan(basePackages = {"alibaba.datafilter", "com.alibaba.cloud.ai"})
@MapperScan("alibaba.datafilter.mapper")
@EnableAsync
@EnableTransactionManagement
public class DataFilterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataFilterApplication.class, args);
    }
}
