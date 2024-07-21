package com.bilanee.octopus.config;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Builder
@Component
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "octopus")
public class OctopusProperties {

    Integer delayUnits;
    String ip;
    Integer djangoPort;
    String username;
    String password;


}
