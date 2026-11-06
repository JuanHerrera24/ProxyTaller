package com.observability.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MicroserviceMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroserviceMonitorApplication.class, args);
    }
}
