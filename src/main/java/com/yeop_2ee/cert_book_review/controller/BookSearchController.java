package com.yeop_2ee.cert_book_review.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@RestController
public class BookSearchController {

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    @GetMapping("/api/books")
    public String searchBooks(@RequestParam String query) {
        log.info("책 검색 요청: " + query);

        // 1. 네이버 API 인증 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 2. URI 빌더로 URL 생성 (인코딩을 한 번만 처리)
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://openapi.naver.com/v1/search/book.json")
                .queryParam("query", query)
                .queryParam("display", 10)
                .encode()
                .build()
                .toUri();

        // 3. 네이버 책 검색 API 호출
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }
}
