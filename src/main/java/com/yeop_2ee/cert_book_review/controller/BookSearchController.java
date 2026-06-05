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

/**
 * 네이버 책 검색 API 프록시 컨트롤러.
 *
 * 역할: 프론트엔드(JavaScript)가 직접 네이버 API를 호출하면 CORS 오류가 발생하므로,
 *       서버가 네이버 API를 대신 호출해 결과를 반환하는 "프록시" 역할을 함.
 *
 * 사용 흐름:
 *  브라우저 → GET /api/books?query=정보처리기사 → 이 컨트롤러 → 네이버 책 검색 API
 *
 * API 키 설정:
 *  application-local.properties에 naver.client-id, naver.client-secret 설정 필요
 *  (네이버 Developers 콘솔에서 발급)
 *
 * @RestController: @Controller + @ResponseBody 합성. 반환값을 JSON/텍스트로 직접 응답.
 */
@Slf4j
@RestController
public class BookSearchController {

    /** application.properties 또는 application-local.properties에서 주입되는 네이버 API 클라이언트 ID */
    @Value("${naver.client-id}")
    private String clientId;

    /** 네이버 API 클라이언트 시크릿 키 */
    @Value("${naver.client-secret}")
    private String clientSecret;

    /**
     * 교재명 키워드로 네이버 책을 검색해 JSON 결과를 반환.
     *
     * GET /api/books?query=검색어
     *
     * 반환 형식: 네이버 책 검색 API 응답 JSON 그대로 전달 (최대 10건)
     * 실패 시: {"items":[]} 빈 결과 반환 (프론트에서 드롭다운 빈 상태로 표시)
     *
     * @param query 검색할 교재명 또는 키워드
     * @return 네이버 API 응답 JSON 문자열
     */
    @GetMapping("/api/books")
    public String searchBooks(@RequestParam String query) {
        try {
            // 네이버 API 인증 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 검색 URL 구성 (UriComponentsBuilder로 쿼리 파라미터 인코딩 처리)
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://openapi.naver.com/v1/search/book.json")
                    .queryParam("query", query)
                    .queryParam("display", 10)  // 최대 10건 반환
                    .encode()
                    .build()
                    .toUri();

            // 네이버 API 호출 및 응답 반환
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            // API 키 미설정 또는 네트워크 오류 시 빈 결과 반환 (서비스 중단 방지)
            log.warn("네이버 책 검색 API 실패 - query: {}, error: {}", query, e.getMessage());
            return "{\"items\":[]}";
        }
    }
}
