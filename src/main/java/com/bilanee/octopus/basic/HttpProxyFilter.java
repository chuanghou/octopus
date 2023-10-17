package com.bilanee.octopus.basic;

import com.stellariver.milky.common.tool.common.Kit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Slf4j
@WebFilter(urlPatterns = "/transfer/*", filterName = "httpProxyFilter")
public class HttpProxyFilter implements Filter {
 
    private String host = "http://118.184.179.116:8002";
 
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("[ {} ] 初始化网关...", this.getClass().getSimpleName());
    }
 
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String requestURI = req.getRequestURI();
        log.info("[ {} ] 接收到请求...URI:{}", this.getClass().getSimpleName(), requestURI);
        requestURI = requestURI.substring("/transfer".length());
        if (!Kit.isBlank(requestURI)) {
            requestURI += "?";
            requestURI += req.getQueryString();
        }
        HttpServletResponse resp = (HttpServletResponse) response;
        // 请求类型
        String method = req.getMethod();
        if (HttpMethod.OPTIONS.matches(method)) {
            chain.doFilter(request, response);
            return;
        }
        HttpMethod httpMethod = HttpMethod.resolve(method);//method
        // 请求头
        MultiValueMap<String, String> headers = parseRequestHeader(req);//header
        // 请求体
        byte[] body = parseRequestBody(req);
        // 封装发singhttp请求
        RequestEntity<?> requestEntity = new RequestEntity<>(body, headers, httpMethod, URI.create(host + requestURI));
        RestTemplate restTemplate = new RestTemplate();
        // 编码格式转换
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> result = restTemplate.exchange(requestEntity, String.class);
        // 将转发请求得到的结果和响应头返回客户端
        String resultBody = result.getBody();
        HttpHeaders resultHeaders = result.getHeaders();
        MediaType contentType = resultHeaders.getContentType();
        if (contentType != null) {
            resp.setContentType(contentType.toString());
        }
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Origin", " https://etsim.pages.dev");
        resp.setHeader("Access-Control-Expose-Headers", "*");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter writer = resp.getWriter();
        writer.write(resultBody);
        writer.flush();
    }
 
    @Override
    public void destroy() {
        log.info("[ {} ] 关闭网关...", this.getClass().getSimpleName());
    }
 
    /**
     * request header
     *
     * @param request
     * @return
     */
    private MultiValueMap<String, String> parseRequestHeader(HttpServletRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        List<String> headerNames = Collections.list(request.getHeaderNames());
        for (String headerName : headerNames) {
            List<String> headerValues = Collections.list(request.getHeaders(headerName));
            for (String headerValue : headerValues) {
                httpHeaders.add(headerName, headerValue);
            }
        }
        return httpHeaders;
    }
 
    /**
     * request body
     *
     * @param request
     * @return
     * @throws IOException
     */
    private byte[] parseRequestBody(HttpServletRequest request) throws IOException {
        InputStream inputStream = request.getInputStream();
        return StreamUtils.copyToByteArray(inputStream);
    }
}