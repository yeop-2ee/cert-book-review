# Cert Book Review

> 자격증 수험생을 위한 교재 리뷰 및 랭킹 서비스

실제 수험생들의 리뷰를 바탕으로 교재 순위, 합격률, 공부 통계를 제공합니다. 로컬 LLM(Ollama)을 활용해 AI 추천 이유 생성, 리뷰 요약, 합격 팁을 외부 API 비용 없이 로컬에서 처리합니다.

---

## 화면 Flow Map

> 실제 구현 화면과 동일한 UI로 제작된 인터랙티브 플로우맵입니다. 아래 링크를 클릭하면 브라우저에서 바로 확인할 수 있습니다.

**[► 화면 Flow Map 보기](https://yeop-2ee.github.io/cert-book-review/cert-book-review-flowmap.html)**

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| **리뷰 작성** | 자격증명 자동완성, 네이버 책 검색, 난이도·합격여부·공부기간·내용 입력 |
| **대시보드 통계** | 총 리뷰 수·자격증 수·전체 합격률·합격자 리뷰 수 카드 |
| **교재 TOP 5 랭킹** | 합격자 기준 최다 사용 교재 순위 |
| **자격증별 요약** | 리뷰 수·합격률·평균 난이도·주요 교재 요약 카드 |
| **교재 랭킹** | 합격자 수·합격률·평균 공부기간 비교 |
| **통계 분석** | 체감 난이도 분포(도넛 차트)·공부기간별 합격률(막대 차트) |
| **AI 분석** | 1위 교재 추천 이유·리뷰 요약·합격 팁 생성 (Ollama 로컬 LLM) |
| **PIN 인증** | 작성 시 설정한 4자리 PIN으로 본인 리뷰 수정·삭제 |
| **최신 합격 후기** | 합격자들의 최신 리뷰 상위 노출 |

---

## 서비스 흐름

```
리뷰 작성
  ├─ 자격증명 자동완성 (Q-net API)
  ├─ 네이버 책 검색으로 교재 선택
  └─ 난이도·합격여부·공부기간·내용·PIN 입력
        ↓
대시보드
  ├─ 전체 통계 카드
  ├─ 합격자 기준 교재 TOP 5
  └─ 최신 합격 후기
        ↓
자격증 선택 → 교재 랭킹 상세
  ├─ AI 추천 이유 생성 (Ollama gemma3:4b)
  └─ 리뷰 요약 생성
        ↓
통계 분석
  ├─ 체감 난이도 분포 (도넛 차트)
  ├─ 공부기간별 합격률 (막대 차트)
  └─ AI 합격 팁 생성
```

### 레이어 구조

```
HTTP 요청
    ↓
Controller  (라우팅·뷰 데이터 준비)
    ↓
Service     (비즈니스 로직·@Transactional)
    ↓
Repository  (Spring Data JPA · H2 DB)
```

---

## 기술 스택

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)](https://openjdk.org)

| 기술 | 버전 | 용도 |
|------|------|------|
| [Spring Boot](https://spring.io/projects/spring-boot) | 3.5 | 웹 프레임워크 |
| [Spring Data JPA](https://spring.io/projects/spring-data-jpa) | - | ORM (CrudRepository) |
| [H2 Database](https://www.h2database.com) | - | 인메모리 데이터베이스 |
| [Mustache](https://mustache.github.io) | - | 서버사이드 템플릿 엔진 |
| [Lombok](https://projectlombok.org) | - | 보일러플레이트 제거 |
| [Bootstrap](https://getbootstrap.com) | 5 | UI 프레임워크 |
| [Chart.js](https://www.chartjs.org) | 4.4.0 | 통계 차트 |
| [Ollama](https://ollama.com) | latest | 로컬 LLM 서버 (gemma3:4b) |
| 네이버 책 검색 API | - | 교재 검색 |
| Q-net 자격증 조회 API | - | 자격증명 자동완성 |

---

## 프로젝트 구조

```
cert-book-review/
│
├── src/main/java/com/yeop_2ee/cert_book_review/
│   ├── client/
│   │   └── OllamaClient.java           # Ollama REST API 클라이언트 (gemma3:4b, 타임아웃 3분)
│   ├── controller/
│   │   ├── ReviewController.java       # 전체 라우팅·AI 엔드포인트 (Service 계층 위임)
│   │   ├── BookSearchController.java   # 네이버 책 검색 API 프록시
│   │   └── CertSearchController.java   # Q-net 자격증 조회 API 프록시
│   ├── service/
│   │   └── ReviewService.java          # 비즈니스 로직·트랜잭션 처리 (Controller ↔ Repository 중간 계층)
│   ├── entity/
│   │   └── Review.java                 # 리뷰 엔티티
│   ├── repository/
│   │   └── ReviewRepository.java       # Spring Data JPA CrudRepository
│   └── dto/
│       ├── ReviewForm.java
│       ├── BookRank.java
│       ├── BookDetail.java
│       ├── CertRanking.java
│       └── CertSummary.java
│
├── src/main/resources/
│   ├── templates/
│   │   ├── layouts/
│   │   │   ├── header.mustache         # 공통 헤더 (CSS·네비게이션)
│   │   │   └── footer.mustache         # 공통 푸터 (JS·차트·애니메이션)
│   │   └── reviews/
│   │       ├── index.mustache          # 대시보드
│   │       ├── show.mustache           # 리뷰 상세
│   │       ├── new.mustache            # 리뷰 작성
│   │       ├── edit.mustache           # 리뷰 수정
│   │       ├── pin-check.mustache      # PIN 인증
│   │       ├── ranking.mustache        # 교재 랭킹 + AI 분석
│   │       └── stats.mustache          # 통계 분석 + AI 합격 팁
│   ├── application.properties          # 앱 설정
│   ├── application-local.properties    # API 키 설정 (gitignore)
│   └── data.sql                        # 초기 샘플 데이터
│
└── build.gradle
```

---

## 시작하기 (로컬 개발)

### 사전 요구사항

| 항목 | 버전 |
|------|------|
| Java | 17+ |
| Gradle | 8+ (또는 포함된 gradlew 사용) |
| Ollama | latest (AI 기능 사용 시) |
| 네이버 Developers 계정 | 책 검색 API 사용 시 |
| Q-net API 키 | 자격증 자동완성 사용 시 |

### 1. 저장소 클론

```bash
git clone <repository-url>
cd cert-book-review
```

### 2. API 키 설정

`src/main/resources/application-local.properties` 파일을 생성합니다.

```properties
naver.client-id=YOUR_NAVER_CLIENT_ID
naver.client-secret=YOUR_NAVER_CLIENT_SECRET
qnet.service-key=YOUR_QNET_SERVICE_KEY
```

> **네이버 API 키**: [Naver Developers](https://developers.naver.com) → 애플리케이션 등록 → 책 검색 API 선택
> **Q-net API 키**: [공공데이터포털](https://www.data.go.kr) → "국가기술자격 종목정보" 검색 후 활용 신청

### 3. Ollama 설정 (AI 기능 사용 시)

```bash
# Ollama 설치 (macOS/Linux)
curl -fsSL https://ollama.com/install.sh | sh

# 모델 다운로드
ollama pull gemma3:4b
```

Ollama는 설치 후 자동으로 백그라운드에서 실행됩니다 (포트 11434).

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

또는 IntelliJ IDEA에서 `CertBookReviewApplication.java`를 직접 실행합니다.

### 5. 접속

| 서비스 | URL |
|--------|-----|
| 애플리케이션 | http://localhost:8080/reviews |
| H2 콘솔 | http://localhost:8080/h2-console |

> H2 콘솔 접속 정보: JDBC URL `jdbc:h2:mem:testdb` / 비밀번호 없음

---

## 엔드포인트

| 경로 | 설명 |
|------|------|
| `GET /reviews` | 대시보드 (통계·TOP 5·최신 후기) |
| `GET /reviews/new` | 리뷰 작성 폼 |
| `POST /reviews/create` | 리뷰 저장 |
| `GET /reviews/{id}` | 리뷰 상세 조회 |
| `GET /reviews/{id}/edit` | 리뷰 수정 폼 (PIN 인증) |
| `GET /reviews/ranking` | 교재 랭킹 |
| `GET /reviews/stats` | 통계 분석 |
| `GET /reviews/ai/recommend-reason` | AI 1위 교재 추천 이유 |
| `GET /reviews/ai/cert-summary` | AI 자격증 리뷰 요약 |
| `GET /reviews/ai/tips` | AI 합격 팁 생성 |

---

## AI 기능 상세

AI 기능은 [Ollama](https://ollama.com)를 통해 **로컬에서** 실행됩니다. 외부 API 비용이 발생하지 않습니다.

| AI 기능 | 모델 | 설명 |
|---------|------|------|
| 1위 교재 추천 이유 | gemma3:4b | 리뷰 데이터 기반 추천 근거 생성 |
| 자격증 리뷰 요약 | gemma3:4b | 전체 리뷰를 한눈에 요약 |
| 합격 팁 | gemma3:4b | 합격자 리뷰에서 핵심 팁 추출 |

다른 모델을 사용하려면 `OllamaClient.java`의 `MODEL` 상수를 변경하세요.
Ollama가 실행되지 않은 경우 AI 기능은 오류 메시지를 반환하며, 나머지 서비스는 정상 동작합니다.
