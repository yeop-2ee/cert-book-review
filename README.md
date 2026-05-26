# cert-book-review

자격증을 준비하는 수험생들을 위한 **교재 리뷰 및 랭킹 서비스**입니다.  
실제 합격자들의 리뷰를 바탕으로 교재 순위, 합격률, 통계 등의 정보를 제공하며,  
로컬 LLM(Ollama)을 활용한 AI 분석 기능을 포함합니다.

---

## 주요 기능

### 리뷰
- **리뷰 작성** : 자격증명(자동완성), 사용 교재(네이버 책 검색), 난이도, 합격 여부, 공부 기간(슬라이더), 리뷰 내용 입력
- **리뷰 목록 / 상세 조회** : 자격증명·교재명 검색, 합격 여부·난이도 필터, 페이지네이션
- **리뷰 수정 / 삭제** : 작성 시 설정한 4자리 PIN 인증 후 수정·삭제

### 대시보드
- **통계 카드** : 총 리뷰 수, 등록 자격증 수, 전체 합격률, 합격자 리뷰 수
- **합격자 기준 교재 TOP 5** : 전체 합격자 수 기준 교재 순위 (자격증명 검색 필터)
- **자격증별 요약** : 자격증별 리뷰 수, 합격률, 평균 난이도, 주요 교재 (검색·페이지네이션)
- **최신 합격 후기** : 최근 등록된 합격 리뷰 상위 노출
- **전체 리뷰 목록** : 자격증·교재·합격여부·난이도 필터 + 페이지네이션

### 교재 랭킹
- 자격증별 교재 순위 (합격자 수 기준), 합격률, 평균 공부기간
- 1위 교재에 ★ 추천 배지 표시
- **AI 분석** : 자격증별 버튼 클릭 시 1위 교재 추천 이유 + 전체 리뷰 요약 자동 생성

### 통계 분석
- 자격증별 체감 난이도 분포 (Chart.js 도넛 차트, 슬라이스 내 퍼센트 표시)
- 공부 기간별 합격률 (가로 막대 차트 + 상세 테이블)
- **AI 합격 팁** : 자격증 선택 조회 시 자동으로 AI 합격 팁 생성 (전체 보기에서는 비표시)

### UI / UX
- 카드 페이드인 + 슬라이드업 애니메이션 (IntersectionObserver 기반, 화면 진입 시 순차 실행)
- 합격률 프로그레스 바 슬라이드인 애니메이션
- 숫자 1,000 단위 콤마 포맷
- 새로고침 시 스크롤 위치 초기화 (scroll restoration 비활성화)
- 반응형 레이아웃 (Bootstrap 5)

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| Framework | Spring Boot 3.1.0 |
| View | Mustache + Bootstrap 5 |
| Database | H2 (In-Memory) |
| ORM | Spring Data JPA (CrudRepository) |
| 외부 API | 네이버 책 검색 API |
| 차트 | Chart.js 4.4.0 + chartjs-plugin-datalabels |
| AI | Ollama (gemma3:12b) — 로컬 LLM |
| 개발 도구 | IntelliJ IDEA |
| 언어 | Java 17 |

---

## AI 기능 사용 조건

AI 분석 기능은 [Ollama](https://ollama.com)가 로컬에서 실행 중이어야 합니다.

```bash
# Ollama 설치 후 모델 실행
ollama run gemma3:12b
```

Ollama가 실행되지 않은 경우 AI 분석 버튼은 오류 메시지를 표시합니다.  
다른 모델 사용 시 `OllamaClient.java`의 `MODEL` 상수를 변경하세요.

---

## 엔티티 구조

**Review**

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 고유 번호 (자동 생성) |
| certName | String | 자격증 이름 |
| bookTitle | String | 사용 교재 이름 |
| difficulty | String | 난이도 (하 / 중 / 상) |
| passed | String | 합격 여부 (Y / N) |
| studyPeriod | String | 공부 기간 |
| content | String | 리뷰 내용 |
| pin | String | 수정/삭제용 4자리 숫자 PIN |

---

## 프로젝트 구조

```
src/main/java/com/yeop_2ee/cert_book_review/
├── client/
│   └── OllamaClient.java       # Ollama REST API 클라이언트
├── controller/
│   └── ReviewController.java   # 전체 라우팅 및 AI 엔드포인트
├── repository/
│   └── ReviewRepository.java
├── entity/
│   └── Review.java
└── dto/
    ├── ReviewForm.java
    ├── BookRank.java
    ├── BookDetail.java
    ├── CertRanking.java
    ├── CertSummary.java
    └── PageItem.java

src/main/resources/
├── templates/
│   ├── layouts/
│   │   ├── header.mustache     # 글로벌 CSS + 네비게이션
│   │   └── footer.mustache     # Bootstrap JS + 애니메이션 스크립트
│   └── reviews/
│       ├── index.mustache      # 대시보드
│       ├── show.mustache       # 리뷰 상세
│       ├── new.mustache        # 리뷰 작성 (슬라이더)
│       ├── edit.mustache       # 리뷰 수정 (슬라이더)
│       ├── pin-check.mustache  # PIN 인증
│       ├── ranking.mustache    # 교재 랭킹 + AI 분석
│       └── stats.mustache      # 통계 분석 + AI 합격 팁
├── application.properties
└── data.sql
```

---

## AI 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/reviews/ai/recommend-reason` | 특정 교재 추천 이유 생성 |
| GET | `/reviews/ai/book-summary` | 특정 교재 리뷰 요약 |
| GET | `/reviews/ai/cert-summary` | 자격증 전체 리뷰 요약 |
| GET | `/reviews/ai/tips` | 자격증 합격 팁 생성 |

---

## 실행 방법

1. 저장소 클론
```bash
git clone https://github.com/yeop-2ee/cert-book-review.git
```

2. (선택) Ollama 실행 — AI 기능 사용 시
```bash
ollama run gemma3:12b
```

3. IntelliJ IDEA에서 프로젝트 열기 후 `CertBookReviewApplication.java` 실행

4. 브라우저에서 접속
```
http://localhost:8080/reviews
```

5. H2 콘솔 (DB 직접 확인)
```
http://localhost:8080/h2-console
JDBC URL : jdbc:h2:mem:testdb
```
