package com.jugbaq.cfp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    ApplicationModules modules = ApplicationModules.of(CallForPapersApplication.class);

    @Test
    void verify_modular_structure() {
        modules.verify();
    }

    @Test
    void print_module_structure() {
        modules.forEach(System.out::println);
    }
}
