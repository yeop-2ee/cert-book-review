package com.yeop_2ee.cert_book_review.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
        try {
            // 1. 짧은 타임아웃으로 RestTemplate 생성 (페이지 렌더링 지연 최소화)
            RestTemplate restTemplate = new RestTemplateBuilder()
                    .connectTimeout(Duration.ofSeconds(3))  // 연결 타임아웃: 3초
                    .readTimeout(Duration.ofSeconds(5))     // 응답 타임아웃: 5초
                    .build();
            // UTF-8 메시지 컨버터를 최우선 순위로 등록
            restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

            // 2. Q-net 공공API 호출 (응답을 byte[]로 받아 인코딩 직접 처리)
            String url = "http://openapi.q-net.or.kr/api/service/rest/InquiryListNationalQualifcationSVC/getList"
                    + "?serviceKey=" + serviceKey
                    + "&numOfRows=500&pageNo=1";  // 한 번에 최대 500개 요청

            ResponseEntity<byte[]> rawResponse = restTemplate.getForEntity(url, byte[].class);
            if (rawResponse.getBody() == null) {
                log.warn("자격증 API 응답 바디가 비어있어 기본 목록을 사용합니다.");
                return DEFAULT_CERT_LIST;
            }

            // 3. 응답 인코딩 처리: Content-Type에서 charset 추출, 없으면 UTF-8 시도
            Charset charset = StandardCharsets.UTF_8;
            if (rawResponse.getHeaders().getContentType() != null
                    && rawResponse.getHeaders().getContentType().getCharset() != null) {
                charset = rawResponse.getHeaders().getContentType().getCharset();
            }
            String jsonResponse = new String(rawResponse.getBody(), charset);

            // UTF-8로 디코딩했는데 JSON 구조 기호({, [)가 없으면 EUC-KR로 재시도
            if (!jsonResponse.contains("{") && !jsonResponse.contains("[")) {
                jsonResponse = new String(rawResponse.getBody(), Charset.forName("EUC-KR"));
            }

            // 4. JSON 파싱: response > body > items > item[] 구조에서 자격증명 추출
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
                log.warn("자격증 API 응답이 비어있어 기본 목록을 사용합니다.");
                return DEFAULT_CERT_LIST;
            }

            return certNames;

        } catch (Exception e) {
            // 타임아웃, 네트워크 오류, JSON 파싱 실패 등 모든 예외 → 기본 목록으로 폴백
            log.warn("자격증 API 연결 실패 - 기본 목록 사용: {}", e.getMessage());
            return DEFAULT_CERT_LIST;
        }
    }
}
