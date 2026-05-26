package com.yeop_2ee.cert_book_review.client;

import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class OllamaClient {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "gemma3:12b";

    private final RestTemplate restTemplate;

    public OllamaClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(180_000); // 3분
        restTemplate = new RestTemplate(factory);
    }

    public String generate(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("prompt", prompt);
            body.put("stream", false);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(OLLAMA_URL, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("response")) {
                return (String) response.getBody().get("response");
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
