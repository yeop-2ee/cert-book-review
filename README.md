# cert-book-review

자격증을 준비하는 수험생들을 위한 **교재 리뷰 및 랭킹 서비스**입니다.  
실제 합격자들의 리뷰를 바탕으로 교재 순위, 합격률, 통계 등의 정보를 제공합니다.

---

## 주요 기능

- **리뷰 작성** : 자격증명(자동완성), 사용 교재(네이버 책 검색), 난이도, 합격 여부, 공부 기간, 리뷰 내용 입력
- **리뷰 목록 / 상세 조회** : 자격증명·교재명 검색, 합격 여부·난이도 필터, 페이지네이션
- **리뷰 수정 / 삭제** : 작성 시 설정한 4자리 PIN 인증 후 수정·삭제
- **통계 대시보드** : 총 리뷰 수, 등록 자격증 수, 전체 합격률, 합격자 리뷰 수 한눈에 표시
- **자격증별 요약** : 자격증별 리뷰 수, 합격률, 평균 난이도, 주요 교재 표시 (검색·페이지네이션)
- **교재 랭킹 & 추천** : 자격증별 교재 순위, 합격자 수, 합격률, 평균 공부기간 통합 제공
- **통계 분석** : 자격증별 난이도 분포(Chart.js 도넛 차트), 공부기간별 합격률(막대 차트)
- **최신 합격 후기** : 최근 등록된 합격 리뷰 상위 노출
- **합격자 기준 교재 TOP 5** : 전체 합격자 리뷰 기준 교재 순위 (자격증명 검색 필터)

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| Framework | Spring Boot 3.1.0 |
| View | Mustache + Bootstrap 5 |
| Database | H2 (In-Memory) |
| ORM | Spring Data JPA (CrudRepository) |
| 외부 API | 네이버 책 검색 API, Q-net 자격증 API |
| 차트 | Chart.js 4.4.0 |
| 개발 도구 | IntelliJ IDEA |
| 언어 | Java 17 |

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
├── controller/
│   └── ReviewController.java
├── repository/
│   └── ReviewRepository.java
├── entity/
│   └── Review.java
└── dto/
    ├── ReviewForm.java
    ├── BookRank.java        # TOP5 교재 랭킹 DTO
    ├── BookDetail.java      # 자격증별 교재 상세 DTO (랭킹+추천 통합)
    ├── CertRanking.java     # 자격증별 교재 목록 DTO
    ├── CertSummary.java     # 자격증 요약 DTO
    └── PageItem.java        # 페이지네이션 DTO

src/main/resources/
├── templates/
│   ├── layouts/
│   │   ├── header.mustache
│   │   └── footer.mustache
│   └── reviews/
│       ├── index.mustache      # 대시보드 (통계 카드, TOP5, 자격증별 요약, 리뷰 목록)
│       ├── show.mustache       # 리뷰 상세
│       ├── new.mustache        # 리뷰 작성
│       ├── edit.mustache       # 리뷰 수정
│       ├── pin-check.mustache  # PIN 인증
│       ├── ranking.mustache    # 교재 랭킹 & 추천
│       └── stats.mustache      # 통계 분석
├── application.properties
└── data.sql                    # 초기 샘플 데이터
```

---

## 실행 방법

1. 저장소 클론
```bash
git clone https://github.com/yeop-2ee/cert-book-review.git
```

2. IntelliJ IDEA에서 프로젝트 열기

3. `CertBookReviewApplication.java` 실행

4. 브라우저에서 접속
```
http://localhost:8080/reviews
```

5. H2 콘솔 (DB 직접 확인)
```
http://localhost:8080/h2-console
JDBC URL : jdbc:h2:mem:testdb
```
