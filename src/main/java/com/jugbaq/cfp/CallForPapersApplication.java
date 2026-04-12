package com.jugbaq.cfp;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@Theme(value = "callforpapers")
public class CallForPapersApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(CallForPapersApplication.class, args);
    }
}
