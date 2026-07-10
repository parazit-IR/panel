package com.parazit.panel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PanelApplication {

    public static void main(String[] args) {
        SpringApplication.run(PanelApplication.class, args);
    }
}
