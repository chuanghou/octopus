package com.bilanee.octopus;

import com.stellariver.milky.spring.partner.wire.EnableStaticWire;
import com.stellariver.milky.starter.EnableMilky;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableMilky
@EnableStaticWire
@SpringBootApplication
public class OctopusApplication {

    public static void main(String[] args) {
        SpringApplication.run(OctopusApplication.class, args);
    }

}
