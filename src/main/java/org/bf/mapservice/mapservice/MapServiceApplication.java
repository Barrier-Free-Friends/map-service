package org.bf.mapservice.mapservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ComponentScan(basePackages = {
        "org.bf.mapservice.mapservice",
        "org.bf.global"
})
public class MapServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MapServiceApplication.class, args);
	}

}
