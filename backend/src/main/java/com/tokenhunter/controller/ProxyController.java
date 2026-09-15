package com.tokenhunter.controller;

import com.tokenhunter.service.proxy.DeepSeekProxyService;
import com.tokenhunter.service.proxy.LmStudioProxyService;
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
    private final LmStudioProxyService lmStudioProxyService;

    public ProxyController(DeepSeekProxyService deepSeekProxyService, LmStudioProxyService lmStudioProxyService) {
        this.deepSeekProxyService = deepSeekProxyService;
        this.lmStudioProxyService = lmStudioProxyService;
    }

    @PostMapping("/proxy/deepseek/**")
    public ResponseEntity<String> forwardDeepSeek(
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

    @PostMapping("/proxy/lmstudio/**")
    public ResponseEntity<String> forwardLmStudio(
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws Exception {

        String uri = request.getRequestURI();
        String path = uri.replace("/proxy/lmstudio", "");

        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        String body = sb.toString();

        log.info("Proxy forward: /proxy/lmstudio{} (body length={})", path, body.length());

        return lmStudioProxyService.forward(path, body, headers);
    }
}
