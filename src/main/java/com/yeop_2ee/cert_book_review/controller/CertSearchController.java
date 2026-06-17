package com.yeop_2ee.cert_book_review.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Q-net(한국산업인력공단) 국가자격증 목록 API 프록시 컨트롤러.
 *
 * 역할: 리뷰 작성 페이지에서 자격증 이름 자동완성(datalist)에 사용할
 *       국가자격증 목록을 공공데이터포털 Q-net API로부터 가져옴.
 *
 * 사용 흐름:
 *  브라우저 → GET /api/certs → 이 컨트롤러 → Q-net 공공API → 자격증 이름 목록 반환
 *
 * 폴백(Fallback) 전략:
 *  API 키 미설정, 타임아웃, 서버 오류 등 모든 예외 상황에서
 *  DEFAULT_CERT_LIST(41개 하드코딩 목록)를 반환해 서비스 연속성 보장.
 *
 * 인코딩 이슈:
 *  Q-net API가 EUC-KR로 응답하는 경우가 있어 바이트 배열로 받아 직접 디코딩 처리.
 */
@Slf4j
@RestController
public class CertSearchController {

    /** application.properties에서 주입되는 Q-net API 서비스 키 (공공데이터포털에서 발급) */
    @Value("${qnet.service-key}")
    private String serviceKey;

    /** 캐시된 자격증 목록 (API 호출 결과를 재사용) */
    private List<String> cachedCertList = null;
    /** 캐시 만료 시각 (24시간마다 재조회) */
    private Instant cacheExpiresAt = Instant.MIN;

    /**
     * API 연결 실패 시 사용할 기본 자격증 목록 (41개).
     * IT, 전기, 건축, 의료, 경영 분야의 주요 국가자격증 포함.
     */
    private static final List<String> DEFAULT_CERT_LIST = Arrays.asList(
            "정보처리기사", "정보처리산업기사", "정보보안기사", "정보보안산업기사",
            "컴퓨터활용능력 1급", "컴퓨터활용능력 2급", "워드프로세서",
            "사무자동화산업기사", "네트워크관리사 1급", "네트워크관리사 2급",
            "리눅스마스터 1급", "리눅스마스터 2급", "빅데이터분석기사",
            "데이터분석준전문가(ADsP)", "데이터분석전문가(ADP)", "SQLD", "SQLP",
            "정보통신기사", "전자계산기조직응용기사", "멀티미디어콘텐츠제작전문가",
            "전산세무 1급", "전산세무 2급", "전산회계 1급", "전산회계 2급",
            "전기기사", "전기산업기사", "전기공사기사", "소방설비기사(전기)",
            "건축기사", "건축산업기사", "토목기사", "토목산업기사",
            "간호사", "임상병리사", "물리치료사", "사회복지사 1급",
            "공인중개사", "주택관리사", "세무사", "공인회계사", "행정사"
    );

    /**
     * 국가자격증 이름 목록을 반환.
     *
     * GET /api/certs
     *
     * 성공 시: Q-net API에서 받은 자격증 이름 목록 (최대 500건)
     * 실패 시: DEFAULT_CERT_LIST 반환
     *
     * @return 자격증 이름 문자열 목록
     */
    @GetMapping("/api/certs")
    public List<String> getCerts() {
        // 캐시 유효 시 즉시 반환 (Q-net API 재호출 없음)
        if (cachedCertList != null && Instant.now().isBefore(cacheExpiresAt)) {
            return cachedCertList;
        }

        try {
            // 1. HttpURLConnection으로 직접 연결 (RestTemplate의 컨버터/에러핸들러 우회)
            String requestUrl = "http://openapi.q-net.or.kr/api/service/rest/InquiryListNationalQualifcationSVC/getList"
                    + "?serviceKey=" + serviceKey
                    + "&numOfRows=500&pageNo=1";

            HttpURLConnection conn = (HttpURLConnection) new URL(requestUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "application/json, */*");

            int status = conn.getResponseCode();
            InputStream inputStream = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            byte[] rawBytes = inputStream.readAllBytes();
            conn.disconnect();

            if (rawBytes == null || rawBytes.length == 0) {
                log.warn("자격증 API 응답 바디가 비어있어 기본 목록을 사용합니다.");
                return DEFAULT_CERT_LIST;
            }

            // 2. 응답 인코딩 처리: UTF-8로 먼저 시도
            String jsonResponse = new String(rawBytes, StandardCharsets.UTF_8);

            // UTF-8로 디코딩했는데 JSON 구조 기호({, [)가 없으면 EUC-KR로 재시도
            if (!jsonResponse.contains("{") && !jsonResponse.contains("[")) {
                jsonResponse = new String(rawBytes, Charset.forName("EUC-KR"));
            }

            // 3. JSON 파싱: response > body > items > item[] 구조에서 자격증명 추출
            List<String> certNames = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);
            // Q-net API 응답 JSON 경로: response.body.items.item[].jmfldnm (자격증 종목명)
            JsonNode items = root.path("response").path("body").path("items").path("item");

            for (JsonNode item : items) {
                String name = item.path("jmfldnm").asText().trim();  // "jmfldnm" = 자격증 종목명 필드
                if (!name.isEmpty()) {
                    certNames.add(name);
                }
            }

            if (certNames.isEmpty()) {
                String resultCode = root.path("response").path("header").path("resultCode").asText("unknown");
                String resultMsg  = root.path("response").path("header").path("resultMsg").asText("unknown");
                log.warn("자격증 API 응답이 비어있어 기본 목록을 사용합니다. resultCode={}, resultMsg={}", resultCode, resultMsg);
                return DEFAULT_CERT_LIST;
            }

            cachedCertList = certNames;
            cacheExpiresAt = Instant.now().plus(Duration.ofHours(24));
            return certNames;

        } catch (Exception e) {
            // 타임아웃, 네트워크 오류, JSON 파싱 실패 등 모든 예외 → 기본 목록으로 폴백
            log.warn("자격증 API 연결 실패 - 기본 목록 사용: {}", e.getMessage());
            return DEFAULT_CERT_LIST;
        }
    }
}
