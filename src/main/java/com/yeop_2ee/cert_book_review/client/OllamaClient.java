package com.yeop_2ee.cert_book_review.client;

import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 로컬 Ollama LLM 서버와 통신하는 HTTP 클라이언트 컴포넌트.
 *
 * Ollama는 PC에 직접 설치해 실행하는 AI 모델 서버임.
 * 외부 AI API(OpenAI 등)와 달리 비용 없이 로컬에서 LLM을 사용 가능.
 *
 * 사용 모델: gemma3:4b (Google Gemma3 4B 파라미터 모델)
 * 실행 주소: http://localhost:11434 (Ollama 기본 포트)
 *
 * 사전 조건:
 *  1. Ollama 설치: https://ollama.com
 *  2. 모델 다운로드: ollama pull gemma3:4b
 *  3. 서버 실행: ollama serve (또는 앱 실행 시 자동 기동)
 */
@Component
public class OllamaClient {

    /** Ollama REST API 엔드포인트 URL (텍스트 생성 요청) */
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    /** 사용할 AI 모델 이름 (ollama pull로 미리 다운로드 필요) */
    private static final String MODEL = "gemma3:4b";

    private final RestTemplate restTemplate;

    /**
     * 생성자: HTTP 요청 타임아웃을 설정한 RestTemplate 초기화.
     * LLM 추론은 시간이 오래 걸릴 수 있으므로 읽기 타임아웃을 3분으로 설정.
     */
    public OllamaClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);   // 연결 타임아웃: 5초
        factory.setReadTimeout(180_000);    // 읽기 타임아웃: 180초 (LLM 추론 시간 고려)
        restTemplate = new RestTemplate(factory);
    }

    /**
     * 주어진 프롬프트를 Ollama에 전송하고 AI 응답 텍스트를 반환.
     *
     * 요청 형식 (POST /api/generate):
     *  { "model": "gemma3:4b", "prompt": "...", "stream": false }
     *
     * 응답 형식:
     *  { "response": "AI가 생성한 텍스트", "done": true, ... }
     *
     * @param prompt AI에게 전달할 프롬프트 텍스트
     * @return AI 응답 텍스트. Ollama 미실행 또는 오류 시 null 반환
     */
    public String generate(String prompt) {
        try {
            // 요청 헤더: JSON 형식 지정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 요청 바디: 모델명, 프롬프트, 스트리밍 여부
            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("prompt", prompt);
            body.put("stream", false);  // false: 응답 완성 후 한 번에 반환 (스트리밍 X)

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // Ollama API 호출
            ResponseEntity<Map> response = restTemplate.postForEntity(OLLAMA_URL, entity, Map.class);

            // 응답 바디에서 "response" 필드 추출
            if (response.getBody() != null && response.getBody().containsKey("response")) {
                return (String) response.getBody().get("response");
            }
        } catch (Exception e) {
            // Ollama 미실행 또는 네트워크 오류 시 null 반환 (컨트롤러에서 에러 메시지 처리)
            return null;
        }
        return null;
    }
}
