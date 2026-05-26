package com.yeop_2ee.cert_book_review.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
public class CertSearchController {

    @Value("${qnet.service-key}")
    private String serviceKey;

    // API 연결 실패 시 사용할 기본 자격증 목록
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

    @GetMapping("/api/certs")
    public List<String> getCerts() {
        try {
            // 1. 타임아웃 설정
            RestTemplate restTemplate = new RestTemplateBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .readTimeout(Duration.ofSeconds(5))
                    .build();

            // 2. Q-net API 호출
            String url = "http://openapi.q-net.or.kr/api/service/rest/InquiryListNationalQualifcationSVC/getList"
                    + "?serviceKey=" + serviceKey
                    + "&numOfRows=500&pageNo=1";

            String jsonResponse = restTemplate.getForObject(url, String.class);

            // 3. JSON 파싱 (API가 JSON으로 응답)
            List<String> certNames = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            for (JsonNode item : items) {
                String name = item.path("jmfldnm").asText().trim();
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
            log.warn("자격증 API 연결 실패 - 기본 목록 사용: {}", e.getMessage());
            return DEFAULT_CERT_LIST;
        }
    }
}
