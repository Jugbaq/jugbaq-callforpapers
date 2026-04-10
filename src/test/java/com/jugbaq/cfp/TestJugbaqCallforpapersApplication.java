package com.jugbaq.cfp;

import org.springframework.boot.SpringApplication;

public class TestJugbaqCallforpapersApplication {

    public static void main(String[] args) {
        SpringApplication.from(CallForPapersApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
