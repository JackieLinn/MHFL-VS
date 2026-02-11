package ynu.jackielinn.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MhflServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MhflServerApplication.class, args);
    }

}
