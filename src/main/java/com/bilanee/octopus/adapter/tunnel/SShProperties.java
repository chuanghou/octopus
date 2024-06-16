package com.bilanee.octopus.adapter.tunnel;

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
@ConfigurationProperties(prefix = "local.ssh")
public class SShProperties {

    String host;
    String port;
    String user;
    String pwd;

}
