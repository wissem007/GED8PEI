package com.edf.gedpei;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application principale GED-PEI WebApp.
 *
 * Gestion des serveurs Linux et Windows pour EDF GED-PEI.
 */
@SpringBootApplication
public class GedPeiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GedPeiApplication.class, args);
    }
}
