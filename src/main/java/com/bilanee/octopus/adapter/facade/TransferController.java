package com.bilanee.octopus.adapter.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequiredArgsConstructor
public class TransferController {

    final RestTemplate restTemplate;

    @RequestMapping("/transfer/**")
    public String mirrorRest(HttpMethod method, HttpServletRequest request) throws URISyntaxException {
        String requestUri = request.getRequestURI().substring("/transfer".length());
        URI uri = new URI("http", null, "106.15.54.213", 8002, requestUri, request.getQueryString(), null);
        ResponseEntity<String> responseEntity = restTemplate.exchange(uri, method, null, String.class);
        return responseEntity.getBody();
    }

}
