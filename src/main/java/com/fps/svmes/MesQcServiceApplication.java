package com.fps.svmes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(scanBasePackages = {
        "com.fps.svmes",
        "com.fps.shared"
}
)
@EntityScan(basePackages = {
        "com.fps.svmes.models.sql",
        "com.fps.shared.entity"
})
@EnableJpaRepositories(basePackages = "com.fps.svmes.repositories.jpaRepo")
@EnableMongoRepositories(basePackages = "com.fps.svmes.repositories.mongoRepo")
@EnableFeignClients
@EnableScheduling
@EnableCaching
public class MesQcServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MesQcServiceApplication.class, args);
	}

}