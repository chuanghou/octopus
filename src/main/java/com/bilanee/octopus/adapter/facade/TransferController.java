package com.bilanee.octopus.adapter.facade;

import com.bilanee.octopus.config.OctopusProperties;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

@CustomLog
@RestController
@RequiredArgsConstructor
public class TransferController {

    private final OctopusProperties octopusProperties;
    private final RestTemplate restTemplate;
    private final Cache<String, String> cache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(1, TimeUnit.MINUTES).build();
    @SneakyThrows
    @RequestMapping("/transfer/**")
    public String mirrorRest(HttpMethod method, HttpServletRequest request) throws URISyntaxException {
        String requestUri = request.getRequestURI().substring("/transfer".length());
        return cache.get(requestUri + request.getQueryString(), () -> {
            URI uri = new URI("http", null, octopusProperties.getIp(), octopusProperties.getDjangoPort(), requestUri, request.getQueryString(), null);
            ResponseEntity<String> responseEntity = restTemplate.exchange(uri, method, null, String.class);
            log.info("restTemplate.exchange(uri, method, null, String.class), response body {}", responseEntity.getBody());
            if (responseEntity.getBody() == null) {
                responseEntity = restTemplate.exchange(uri, method, null, String.class);
                log.info("restTemplate.exchange(uri, method, null, String.class), response body {}", responseEntity.getBody());
            }
            return responseEntity.getBody();
        });
    }


}
