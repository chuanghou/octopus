package com.bilanee.octopus.adapter.ws;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WsMessage {
    WsTopic wsTopic;
    Object body;
}
