package com.bilanee.octopus.adapter.facade;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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

@RestController
@RequiredArgsConstructor
public class TransferController {

    private final RestTemplate restTemplate;
    private final Cache<String, String> cache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(1, TimeUnit.MINUTES).build();
    @SneakyThrows
    @RequestMapping("/transfer/**")
    public String mirrorRest(HttpMethod method, HttpServletRequest request) throws URISyntaxException {
        String requestUri = request.getRequestURI().substring("/transfer".length());
        return cache.get(requestUri, () -> {
            URI uri = new URI("http", null, "106.15.54.213", 8002, requestUri, request.getQueryString(), null);
            ResponseEntity<String> responseEntity = restTemplate.exchange(uri, method, null, String.class);
            return responseEntity.getBody();
        });
    }

}
