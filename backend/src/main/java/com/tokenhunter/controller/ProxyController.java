package com.tokenhunter.controller;

import com.tokenhunter.service.proxy.DeepSeekProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;

@RestController
public class ProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

    private final DeepSeekProxyService deepSeekProxyService;

    public ProxyController(DeepSeekProxyService deepSeekProxyService) {
        this.deepSeekProxyService = deepSeekProxyService;
    }

    @PostMapping("/proxy/deepseek/**")
    public ResponseEntity<String> forward(
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws Exception {

        String uri = request.getRequestURI();
        String path = uri.replace("/proxy/deepseek", "");

        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        String body = sb.toString();

        log.info("Proxy forward: /proxy/deepseek{} (body length={})", path, body.length());

        return deepSeekProxyService.forward(path, body, headers);
    }
}
