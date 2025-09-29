package alibaba.datafilter;

import alibaba.datafilter.common.concurrent.UserHolder;
import alibaba.datafilter.model.dto.UserDTO;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("alibaba.datafilter.mapper")
public class DataFilterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataFilterApplication.class, args);

    }

}
