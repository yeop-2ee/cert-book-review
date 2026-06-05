package com.yeop_2ee.cert_book_review.controller;

import com.yeop_2ee.cert_book_review.client.OllamaClient;
import com.yeop_2ee.cert_book_review.dto.BookDetail;
import com.yeop_2ee.cert_book_review.dto.BookRank;
import com.yeop_2ee.cert_book_review.dto.CertRanking;
import com.yeop_2ee.cert_book_review.dto.CertSummary;
import com.yeop_2ee.cert_book_review.dto.ReviewForm;
import com.yeop_2ee.cert_book_review.entity.Review;
import com.yeop_2ee.cert_book_review.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/**
 * 핵심 웹 컨트롤러 - 리뷰 CRUD, 통계, 랭킹, AI 기능의 모든 요청을 처리.
 *
 * 계층 구조: Controller → Service → Repository → DB → View
 *  - 이 클래스는 HTTP 요청/응답과 View 데이터 준비만 담당
 *  - 비즈니스 로직(CRUD, 트랜잭션)은 ReviewService에 위임
 *
 * 주요 기능:
 *  1. 대시보드 (GET /reviews)        : 전체 통계, TOP5, 최신 합격 후기, 자격증별 요약
 *  2. 리뷰 작성 (GET/POST /reviews/new + /reviews/create)
 *  3. 리뷰 상세 (GET /reviews/{id})
 *  4. PIN 인증  (GET/POST /reviews/{id}/pin-check) : 수정·삭제 전 PIN 확인
 *  5. 리뷰 수정 (GET /reviews/{id}/edit + POST /reviews/update)
 *  6. 교재 랭킹 (GET /reviews/ranking) : 자격증별 교재 합격자 순위
 *  7. 통계 분석 (GET /reviews/stats)   : 난이도·공부기간별 합격률 차트 데이터
 *  8. AI 기능   (GET /reviews/ai/*)    : Ollama LLM으로 추천 이유, 합격 팁, 요약 생성
 *
 * @Slf4j      : log.info/warn/error() 로그 사용 가능하게 설정
 * @Controller : 뷰(Mustache 템플릿)를 반환하는 MVC 컨트롤러
 */
@Slf4j
@Controller
public class ReviewController {

    /**
     * 비즈니스 로직 서비스 (Spring이 자동으로 주입).
     * CRUD 작업은 reviewRepository 대신 reviewService를 통해서만 수행.
     * Controller → Service → Repository 계층 구조 준수.
     */
    @Autowired
    private ReviewService reviewService;

    /** 로컬 Ollama AI 서버 통신 클라이언트 (Spring이 자동으로 주입) */
    @Autowired
    private OllamaClient ollamaClient;

    /**
     * 대시보드 페이지.
     * GET /reviews
     *
     * reviewService.findAll()로 전체 리뷰를 조회한 뒤,
     * 4가지 통계를 메모리에서 계산해 모델에 담음.
     * 실제 필터링/페이지네이션은 클라이언트 JS에서 처리 (서버 부하 최소화).
     */
    @GetMapping("/reviews")
    public String index(Model model) {
        // Service를 통해 전체 리뷰 조회 (Controller → Service → Repository)
        ArrayList<Review> allReviews = reviewService.findAll();

        // ── 1. 전체 기준 통계 카드 4개 ───────────────────────────────
        int total = allReviews.size();  // 총 리뷰 수
        int passedCount = 0;
        List<String> uniqueCerts = new ArrayList<>();
        for (Review r : allReviews) {
            if ("Y".equals(r.getPassed())) passedCount++;  // 합격자 수 카운트
            if (!uniqueCerts.contains(r.getCertName())) uniqueCerts.add(r.getCertName());  // 자격증 종류 중복 제거
        }
        // 전체 합격률 (정수, 소수점 버림)
        int passRateNum = total > 0 ? (passedCount * 100 / total) : 0;

        // ── 2. 교재 랭킹 TOP 5 (합격자 기준) ──────────────────────────────
        // key: "자격증명|교재명", value: [합격자 수]
        Map<String, int[]> top5Map = new LinkedHashMap<>();
        for (Review r : allReviews) {
            if ("Y".equals(r.getPassed())) {
                String key = r.getCertName() + "|" + r.getBookTitle();
                if (!top5Map.containsKey(key)) top5Map.put(key, new int[]{0});
                top5Map.get(key)[0]++;
            }
        }
        // Map → BookRank 리스트 변환 후 합격자 수 내림차순 정렬
        List<BookRank> rankList = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : top5Map.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            rankList.add(new BookRank(0, parts[0], parts[1], entry.getValue()[0]));
        }
        rankList.sort((a, b) -> b.getCount() - a.getCount());
        // 순위 번호 재부여 (1위부터)
        for (int i = 0; i < rankList.size(); i++) {
            BookRank r = rankList.get(i);
            rankList.set(i, new BookRank(i + 1, r.getCertName(), r.getBookTitle(), r.getCount()));
        }
        List<BookRank> top5RankList = rankList.size() > 5 ? rankList.subList(0, 5) : rankList;

        // ── 3. 최신 합격 후기 최대 5개 (최근 등록순) ────────────────────────
        // 리스트 뒤에서부터 순회해 passed="Y"인 리뷰 5개 추출 (최신순)
        List<Review> recentPassedList = new ArrayList<>();
        for (int i = allReviews.size() - 1; i >= 0; i--) {
            if ("Y".equals(allReviews.get(i).getPassed())) {
                recentPassedList.add(allReviews.get(i));
                if (recentPassedList.size() == 5) break;
            }
        }

        // ── 4. 자격증별 요약 계산 ───────────────────────────────────────
        // 자격증 이름으로 그룹핑
        Map<String, List<Review>> certGroup = new LinkedHashMap<>();
        for (Review r : allReviews) {
            if (!certGroup.containsKey(r.getCertName()))
                certGroup.put(r.getCertName(), new ArrayList<>());
            certGroup.get(r.getCertName()).add(r);
        }
        List<CertSummary> certSummaryList = new ArrayList<>();
        for (Map.Entry<String, List<Review>> entry : certGroup.entrySet()) {
            String certName = entry.getKey();
            List<Review> certReviews = entry.getValue();
            int certTotal = certReviews.size();
            int certPassed = 0, diffSum = 0;
            Map<String, Integer> bookCount = new LinkedHashMap<>();
            for (Review r : certReviews) {
                if ("Y".equals(r.getPassed())) certPassed++;
                // 난이도를 숫자로 환산 (하=1, 중=2, 상=3)해서 평균 계산
                if ("하".equals(r.getDifficulty())) diffSum += 1;
                else if ("중".equals(r.getDifficulty())) diffSum += 2;
                else if ("상".equals(r.getDifficulty())) diffSum += 3;
                // 교재별 리뷰 수 카운트 (가장 많이 언급된 교재 = 대표 교재)
                bookCount.put(r.getBookTitle(), bookCount.getOrDefault(r.getBookTitle(), 0) + 1);
            }
            int certPassRateNum = certTotal > 0 ? (certPassed * 100 / certTotal) : 0;
            // 평균 난이도 텍스트 변환 (1.0~1.5 → "하", 1.5~2.5 → "중", 2.5~3.0 → "상")
            double avgDiff = (double) diffSum / certTotal;
            String avgDifficulty = avgDiff <= 1.5 ? "하" : avgDiff <= 2.5 ? "중" : "상";
            // 가장 리뷰 수 많은 교재 = 대표 교재
            String topBook = "-";
            int maxCnt = 0;
            for (Map.Entry<String, Integer> b : bookCount.entrySet()) {
                if (b.getValue() > maxCnt) { maxCnt = b.getValue(); topBook = b.getKey(); }
            }
            certSummaryList.add(new CertSummary(
                    certName, certTotal, certPassed,
                    certPassRateNum + "%", certPassRateNum,
                    avgDifficulty, topBook));
        }

        // ── 5. 템플릿에 전달할 데이터 등록 ────────────────────────────────
        // "%,d" 포맷: 천단위 콤마 적용된 문자열 (예: 1000 → "1,000")
        model.addAttribute("total", total);
        model.addAttribute("totalFmt", String.format("%,d", total));
        model.addAttribute("certCount", uniqueCerts.size());
        model.addAttribute("certCountFmt", String.format("%,d", uniqueCerts.size()));
        model.addAttribute("passedCount", passedCount);
        model.addAttribute("passedCountFmt", String.format("%,d", passedCount));
        model.addAttribute("passRate", passRateNum + "%");
        model.addAttribute("top5RankList", top5RankList);
        model.addAttribute("recentPassedList", recentPassedList);
        model.addAttribute("certSummaryList", certSummaryList);
        model.addAttribute("reviewList", allReviews);  // 전체 리뷰 (JS 클라이언트 필터링용)

        return "reviews/index";
    }

    /**
     * /reviews/index → /reviews 리다이렉트 (URL 별칭).
     */
    @GetMapping("/reviews/index")
    public String indexAlias() {
        return "redirect:/reviews";
    }

    /**
     * 리뷰 작성 폼 페이지.
     * GET /reviews/new
     */
    @GetMapping("/reviews/new")
    public String newReview() {
        return "reviews/new";
    }

    /**
     * 리뷰 저장 처리 (CREATE).
     * POST /reviews/create
     *
     * 폼 데이터를 ReviewForm으로 자동 바인딩 받아 ReviewService.create()로 저장.
     * Service 내부에서 @Transactional이 적용되어 트랜잭션 보장.
     * 저장된 리뷰의 상세 페이지로 리다이렉트.
     */
    @PostMapping("/reviews/create")
    public String create(ReviewForm form) {
        // Controller → Service → Repository 계층 구조: 비즈니스 로직은 Service에 위임
        Review saved = reviewService.create(form);
        return "redirect:/reviews/" + saved.getId();  // 저장된 리뷰 상세 페이지로 이동
    }

    /**
     * 리뷰 상세 조회 페이지 (READ).
     * GET /reviews/{id}
     *
     * @param id URL 경로의 리뷰 ID (@PathVariable: URL 경로 변수를 파라미터로 바인딩)
     */
    @GetMapping("/reviews/{id}")
    public String show(@PathVariable Long id, Model model) {
        // Service를 통해 단건 조회 (없으면 null)
        Review review = reviewService.findById(id).orElse(null);
        model.addAttribute("review", review);
        return "reviews/show";
    }

    /**
     * PIN 인증 폼 페이지 (수정/삭제 전 PIN 입력 요구).
     * GET /reviews/{id}/pin-check?action=edit 또는 action=delete
     *
     * @param action "edit"(수정) 또는 "delete"(삭제)
     */
    @GetMapping("/reviews/{id}/pin-check")
    public String pinCheckForm(@PathVariable Long id, @RequestParam String action, Model model) {
        model.addAttribute("id", id);
        model.addAttribute("action", action);
        return "reviews/pin-check";
    }

    /**
     * PIN 검증 처리.
     * POST /reviews/{id}/pin-check
     *
     * 입력한 PIN이 DB 저장 PIN과 일치하면 → 수정 또는 삭제 진행
     * 불일치하면 → PIN 입력 폼으로 돌아가 오류 메시지 표시
     *
     * @param action "edit" 또는 "delete"
     * @param pin    사용자가 입력한 PIN
     * @param rttr   리다이렉트 시 일회성 메시지 전달용 (Flash Attribute, ch.8 기법)
     */
    @PostMapping("/reviews/{id}/pin-check")
    public String pinCheck(@PathVariable Long id,
                           @RequestParam String action,
                           @RequestParam String pin,
                           RedirectAttributes rttr) {
        Review review = reviewService.findById(id).orElse(null);
        if (review == null) return "redirect:/reviews";  // 리뷰가 없으면 목록으로

        // PIN 불일치: Flash Attribute로 오류 메시지 전달 후 PIN 입력 폼으로 리다이렉트
        if (!review.getPin().equals(pin)) {
            rttr.addFlashAttribute("errorMsg", "PIN이 올바르지 않습니다.");
            return "redirect:/reviews/" + id + "/pin-check?action=" + action;
        }

        // PIN 일치 + 삭제 요청: Service를 통해 DB에서 삭제 후 목록으로
        if ("delete".equals(action)) {
            reviewService.delete(id);  // @Transactional 적용된 Service 메서드 호출
            rttr.addFlashAttribute("msg", "리뷰가 삭제되었습니다!");
            return "redirect:/reviews";
        }

        // PIN 일치 + 수정 요청: 수정 폼으로 이동
        return "redirect:/reviews/" + id + "/edit";
    }

    /**
     * 리뷰 수정 폼 페이지 (UPDATE - GET).
     * GET /reviews/{id}/edit
     * (pin-check를 통해서만 접근 가능)
     */
    @GetMapping("/reviews/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Review review = reviewService.findById(id).orElse(null);
        model.addAttribute("review", review);
        return "reviews/edit";
    }

    /**
     * 리뷰 수정 저장 처리 (UPDATE - POST).
     * POST /reviews/update
     *
     * ReviewService.update()를 호출해 기존 리뷰를 덮어씌움.
     * Service 내부에서 @Transactional이 적용되어 트랜잭션 보장.
     */
    @PostMapping("/reviews/update")
    public String update(ReviewForm form) {
        // Controller → Service → Repository 계층 구조: 수정 로직은 Service에 위임
        Review updated = reviewService.update(form);
        if (updated == null) return "redirect:/reviews";  // 대상 없으면 목록으로
        return "redirect:/reviews/" + updated.getId();
    }

    /**
     * 통계 분석 페이지 (READ).
     * GET /reviews/stats?certName=자격증명  (certName 미입력 시 전체 통계)
     *
     * 자격증 선택 → 해당 자격증 리뷰만 필터링 → Chart.js 차트 데이터 계산.
     *
     * @param certName 필터링할 자격증 이름 (빈 문자열이면 전체)
     */
    @GetMapping("/reviews/stats")
    public String stats(@RequestParam(defaultValue = "") String certName, Model model) {
        ArrayList<Review> allReviews = reviewService.findAll();

        // 드롭다운에 표시할 자격증 이름 목록 (중복 제거)
        List<String> certNames = new ArrayList<>();
        for (Review r : allReviews) {
            if (!certNames.contains(r.getCertName())) certNames.add(r.getCertName());
        }

        // 선택한 자격증으로 필터링 (미선택 시 전체 리뷰 사용)
        List<Review> filtered = new ArrayList<>();
        for (Review r : allReviews) {
            if (certName.isEmpty() || certName.equals(r.getCertName())) filtered.add(r);
        }

        // 난이도 분포 카운트 (Chart.js 도넛 차트용)
        int diffLow = 0, diffMid = 0, diffHigh = 0;
        for (Review r : filtered) {
            if ("하".equals(r.getDifficulty())) diffLow++;
            else if ("중".equals(r.getDifficulty())) diffMid++;
            else if ("상".equals(r.getDifficulty())) diffHigh++;
        }

        // 공부 기간별 합격률 계산 (Chart.js 막대 차트용)
        // 구간: [0]=1개월 미만, [1]=1~3개월, [2]=3~6개월, [3]=6개월 이상
        int[] pCount = {0, 0, 0, 0};   // 각 구간 총 리뷰 수
        int[] pPassed = {0, 0, 0, 0};  // 각 구간 합격 리뷰 수
        for (Review r : filtered) {
            int months = parseMonths(r.getStudyPeriod());  // 텍스트 → 개월 수 변환
            int g = months < 1 ? 0 : months <= 3 ? 1 : months <= 6 ? 2 : 3;  // 구간 인덱스
            pCount[g]++;
            if ("Y".equals(r.getPassed())) pPassed[g]++;
        }

        model.addAttribute("certName", certName);
        model.addAttribute("hasCertName", !certName.isEmpty());  // 자격증 선택 여부 (Mustache 분기용)
        model.addAttribute("certNames", certNames);
        model.addAttribute("totalReviewsFmt", String.format("%,d", filtered.size()));
        model.addAttribute("diffLow", diffLow);
        model.addAttribute("diffMid", diffMid);
        model.addAttribute("diffHigh", diffHigh);
        // pr0~pr3: 각 기간 구간의 합격률 %, pc0~pc3: 각 기간 구간의 총 리뷰 수
        model.addAttribute("pr0", pCount[0] > 0 ? pPassed[0] * 100 / pCount[0] : 0);
        model.addAttribute("pr1", pCount[1] > 0 ? pPassed[1] * 100 / pCount[1] : 0);
        model.addAttribute("pr2", pCount[2] > 0 ? pPassed[2] * 100 / pCount[2] : 0);
        model.addAttribute("pr3", pCount[3] > 0 ? pPassed[3] * 100 / pCount[3] : 0);
        model.addAttribute("pc0", pCount[0]);
        model.addAttribute("pc1", pCount[1]);
        model.addAttribute("pc2", pCount[2]);
        model.addAttribute("pc3", pCount[3]);
        model.addAttribute("hasData", !filtered.isEmpty());  // 데이터 없을 때 안내 메시지 분기용
        return "reviews/stats";
    }

    /**
     * 추천 자격증 → 랭킹 페이지 리다이렉트 (자격증 미입력 시 전체 랭킹).
     * GET /reviews/recommend?certName=자격증명
     */
    @GetMapping("/reviews/recommend")
    public String recommend(@RequestParam(defaultValue = "") String certName) {
        if (certName.isEmpty()) return "redirect:/reviews/ranking";
        return "redirect:/reviews/ranking?certName=" + certName;
    }

    /**
     * 공부 기간 텍스트를 개월 수(정수)로 변환하는 유틸 메서드.
     *
     * 변환 규칙:
     *  "1년"     → 12개월
     *  "6개월"   → 6개월
     *  "3주"     → max(1, 3/4) = 1개월 (1개월 미만은 1로 올림)
     *  "30일"    → max(1, 30/30) = 1개월
     *  파싱 불가 → 0 반환
     *
     * @param period 공부 기간 텍스트 (예: "3개월", "1년", "2주")
     * @return 개월 수 (정수)
     */
    private int parseMonths(String period) {
        if (period == null || period.isEmpty()) return 0;
        try {
            String num = period.replaceAll("[^0-9]", "");  // 숫자만 추출
            if (num.isEmpty()) return 0;
            int n = Integer.parseInt(num);
            if (period.contains("년")) return n * 12;
            if (period.contains("개월") || period.contains("달")) return n;
            if (period.contains("주")) return Math.max(1, n / 4);
            if (period.contains("일")) return Math.max(1, n / 30);
        } catch (Exception e) { /* 파싱 실패 무시, 0 반환 */ }
        return 0;
    }

    /**
     * 교재 랭킹 페이지 (READ).
     * GET /reviews/ranking
     *
     * 모든 리뷰를 자격증별로 그룹핑하고,
     * 각 자격증 내에서 교재별 합격자 수를 기준으로 순위를 매김.
     */
    @GetMapping("/reviews/ranking")
    public String ranking(Model model) {
        ArrayList<Review> reviewList = reviewService.findAll();

        // 2단계 Map: 자격증명 → 교재명 → [총리뷰수, 합격수, 총기간(개월)합계, 기간있는리뷰수]
        Map<String, Map<String, int[]>> certBookMap = new LinkedHashMap<>();
        for (Review r : reviewList) {
            String cert = r.getCertName();
            String book = r.getBookTitle();
            if (!certBookMap.containsKey(cert)) certBookMap.put(cert, new LinkedHashMap<>());
            Map<String, int[]> bookMap = certBookMap.get(cert);
            if (!bookMap.containsKey(book)) bookMap.put(book, new int[]{0, 0, 0, 0});
            int[] s = bookMap.get(book);
            s[0]++;  // 총 리뷰 수 증가
            if ("Y".equals(r.getPassed())) s[1]++;  // 합격 수 증가
            int months = parseMonths(r.getStudyPeriod());
            if (months > 0) { s[2] += months; s[3]++; }  // 평균 공부기간 계산용
        }

        // 자격증별 교재 순위 목록 생성
        List<CertRanking> certRankingList = new ArrayList<>();
        for (Map.Entry<String, Map<String, int[]>> certEntry : certBookMap.entrySet()) {
            List<BookDetail> books = new ArrayList<>();
            for (Map.Entry<String, int[]> bookEntry : certEntry.getValue().entrySet()) {
                int[] s = bookEntry.getValue();
                int rateNum = s[0] > 0 ? s[1] * 100 / s[0] : 0;  // 합격률 계산
                String avgPeriod = s[3] > 0 ? (s[2] / s[3]) + "개월" : "-";  // 평균 공부기간
                books.add(new BookDetail(0, bookEntry.getKey(), s[1], s[0], rateNum + "%", rateNum, avgPeriod));
            }
            // 합격자 수 내림차순 정렬 후 순위 번호 부여
            books.sort((a, b) -> b.getPassedCount() - a.getPassedCount());
            for (int i = 0; i < books.size(); i++) {
                BookDetail b = books.get(i);
                books.set(i, new BookDetail(i + 1, b.getBookTitle(), b.getPassedCount(), b.getTotalCount(),
                        b.getPassRate(), b.getPassRateNum(), b.getAvgStudyPeriod()));
            }
            certRankingList.add(new CertRanking(certEntry.getKey(), books));
        }

        model.addAttribute("certRankingList", certRankingList);
        return "reviews/ranking";
    }

    /**
     * AI: 1위 교재 추천 이유 생성.
     * GET /reviews/ai/recommend-reason?certName=자격증명&bookTitle=교재명
     *
     * 해당 교재의 통계(합격률, 평균 공부기간)를 Ollama 프롬프트에 담아
     * 추천 이유 3~4문장을 생성해 JSON으로 반환.
     *
     * ResponseEntity<Map<String,String>>: HTTP 상태코드를 명시적으로 지정해 반환
     *   → 성공: 200 OK + { "result": "AI 추천 이유 텍스트" }
     *   → 실패: 500 Internal Server Error + { "error": "오류 메시지" }
     */
    @GetMapping("/reviews/ai/recommend-reason")
    public ResponseEntity<Map<String, String>> aiRecommendReason(@RequestParam String certName,
                                                                  @RequestParam String bookTitle) {
        try {
            ArrayList<Review> all = reviewService.findAll();
            // 해당 자격증 + 교재의 통계 집계
            int total = 0, passed = 0, periodSum = 0, periodCount = 0;
            for (Review r : all) {
                if (certName.equals(r.getCertName()) && bookTitle.equals(r.getBookTitle())) {
                    total++;
                    if ("Y".equals(r.getPassed())) passed++;
                    int m = parseMonths(r.getStudyPeriod());
                    if (m > 0) { periodSum += m; periodCount++; }
                }
            }
            int passRate = total > 0 ? passed * 100 / total : 0;
            String avgPeriod = periodCount > 0 ? (periodSum / periodCount) + "개월" : "정보 없음";

            // AI 프롬프트 구성: 통계 데이터를 자연어로 서술하도록 유도
            String prompt = "당신은 자격증 시험 전문 조언가입니다. 다음 데이터를 바탕으로 이 교재를 추천하는 이유를 한국어로 3~4문장으로 간결하게 작성해주세요.\n\n"
                    + "자격증: " + certName + "\n"
                    + "교재명: " + bookTitle + "\n"
                    + "총 리뷰 수: " + total + "개\n"
                    + "합격자 수: " + passed + "명\n"
                    + "합격률: " + passRate + "%\n"
                    + "평균 공부 기간: " + avgPeriod + "\n\n"
                    + "마크다운 기호 없이 일반 텍스트로만 작성해주세요.";

            // 성공 시 200 OK 응답
            return ResponseEntity.ok(buildAiResponse(ollamaClient.generate(prompt)));
        } catch (Exception e) {
            log.error("AI 교재 추천 이유 오류 - certName: {}, bookTitle: {}", certName, bookTitle, e);
            Map<String, String> err = new HashMap<>();
            err.put("error", "서버 오류: " + e.getMessage());
            // 실패 시 500 Internal Server Error 응답
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * AI: 합격 팁 생성.
     * GET /reviews/ai/tips?certName=자격증명  (certName 없으면 전체 기준)
     *
     * 통계(합격률, 난이도 분포, 공부기간별 합격률)를 기반으로
     * 구체적인 공부 팁 3~5개를 번호 목록 형태로 생성.
     *
     * ResponseEntity<Map<String,String>>: HTTP 상태코드를 명시적으로 지정해 반환
     *   → 성공: 200 OK + { "result": "팁 텍스트" }
     *   → 데이터 없음: 404 Not Found + { "error": "데이터가 없습니다." }
     *   → 서버 오류: 500 Internal Server Error + { "error": "..." }
     */
    @GetMapping(value = "/reviews/ai/tips", produces = "application/json")
    public ResponseEntity<Map<String, String>> aiTips(@RequestParam(defaultValue = "") String certName) {
        try {
            ArrayList<Review> all = reviewService.findAll();
            // 선택 자격증 필터링 (미선택 시 전체)
            List<Review> filtered = new ArrayList<>();
            for (Review r : all) {
                if (certName.isEmpty() || certName.equals(r.getCertName())) filtered.add(r);
            }
            if (filtered.isEmpty()) {
                Map<String, String> err = new HashMap<>();
                err.put("error", "데이터가 없습니다.");
                // 데이터 없음: 404 Not Found 응답
                return ResponseEntity.notFound().build();
            }
            // 통계 집계
            int total = filtered.size(), passed = 0, dLow = 0, dMid = 0, dHigh = 0;
            int[] pCount = {0, 0, 0, 0}, pPassed = {0, 0, 0, 0};
            for (Review r : filtered) {
                if ("Y".equals(r.getPassed())) passed++;
                if ("하".equals(r.getDifficulty())) dLow++;
                else if ("중".equals(r.getDifficulty())) dMid++;
                else if ("상".equals(r.getDifficulty())) dHigh++;
                int m = parseMonths(r.getStudyPeriod());
                int g = m < 1 ? 0 : m <= 3 ? 1 : m <= 6 ? 2 : 3;
                pCount[g]++;
                if ("Y".equals(r.getPassed())) pPassed[g]++;
            }
            int passRate = total > 0 ? passed * 100 / total : 0;
            String target = certName.isEmpty() ? "이 자격증" : "\"" + certName + "\"";
            // AI 프롬프트: 통계 기반 맞춤 공부 팁 요청
            String prompt = "당신은 자격증 시험 전문 조언가입니다. 다음 통계 데이터를 바탕으로 " + target
                    + " 합격을 위한 구체적인 공부 팁을 한국어로 3~5가지 번호 항목으로 작성해주세요.\n\n"
                    + "총 리뷰 수: " + total + "개\n"
                    + "전체 합격률: " + passRate + "%\n"
                    + "체감 난이도: 하(" + dLow + "명) 중(" + dMid + "명) 상(" + dHigh + "명)\n"
                    + "공부 기간별 합격률:\n"
                    + "- 1개월 미만: " + (pCount[0] > 0 ? pPassed[0] * 100 / pCount[0] : 0) + "% (" + pCount[0] + "명)\n"
                    + "- 1~3개월: " + (pCount[1] > 0 ? pPassed[1] * 100 / pCount[1] : 0) + "% (" + pCount[1] + "명)\n"
                    + "- 3~6개월: " + (pCount[2] > 0 ? pPassed[2] * 100 / pCount[2] : 0) + "% (" + pCount[2] + "명)\n"
                    + "- 6개월 이상: " + (pCount[3] > 0 ? pPassed[3] * 100 / pCount[3] : 0) + "% (" + pCount[3] + "명)\n\n"
                    + "마크다운 기호 없이 번호 목록 형식의 일반 텍스트로만 작성해주세요.";

            // 성공 시 200 OK 응답
            return ResponseEntity.ok(buildAiResponse(ollamaClient.generate(prompt)));
        } catch (Exception e) {
            log.error("AI 합격 팁 생성 오류 - certName: {}", certName, e);
            Map<String, String> err = new HashMap<>();
            err.put("error", "서버 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * AI: 자격증 전체 리뷰 요약.
     * GET /reviews/ai/cert-summary?certName=자격증명
     *
     * 해당 자격증의 실제 리뷰 내용(최대 15개)을 수집해 Ollama에 전달하고,
     * 시험 특징·난이도·수험 경향을 3~4문장으로 요약한 텍스트를 반환.
     *
     * ResponseEntity<Map<String,String>>: HTTP 상태코드를 명시적으로 지정해 반환
     *   → 성공: 200 OK + { "result": "요약 텍스트" }
     *   → 데이터 없음: 404 Not Found
     *   → 서버 오류: 500 Internal Server Error
     */
    @GetMapping("/reviews/ai/cert-summary")
    public ResponseEntity<Map<String, String>> aiCertSummary(@RequestParam String certName) {
        try {
            ArrayList<Review> all = reviewService.findAll();
            // 해당 자격증의 리뷰 내용 수집 (합격/불합격·난이도 태그 포함)
            List<String> contents = new ArrayList<>();
            for (Review r : all) {
                if (certName.equals(r.getCertName()) && r.getContent() != null && !r.getContent().isBlank()) {
                    String tag = "Y".equals(r.getPassed()) ? "합격" : "불합격";
                    contents.add("- [" + tag + "/" + r.getDifficulty() + "] " + r.getContent().trim());
                }
            }
            if (contents.isEmpty()) {
                // 데이터 없음: 404 Not Found 응답
                return ResponseEntity.notFound().build();
            }
            // 프롬프트 길이 제한: 최대 15개 리뷰만 사용 (LLM 컨텍스트 윈도우 초과 방지)
            List<String> limited = contents.size() > 15 ? contents.subList(0, 15) : contents;
            String prompt = "당신은 자격증 시험 전문 조언가입니다. 다음은 \"" + certName + "\" 자격증에 대한 수험생들의 실제 리뷰입니다. "
                    + "이 리뷰들을 종합하여 이 자격증 시험의 특징, 난이도, 수험 경향을 한국어로 3~4문장으로 요약해주세요.\n\n"
                    + "리뷰 내용:\n" + String.join("\n", limited) + "\n\n"
                    + "마크다운 기호 없이 일반 텍스트로만 작성해주세요.";

            // 성공 시 200 OK 응답
            return ResponseEntity.ok(buildAiResponse(ollamaClient.generate(prompt)));
        } catch (Exception e) {
            log.error("AI 자격증 리뷰 요약 오류 - certName: {}", certName, e);
            Map<String, String> err = new HashMap<>();
            err.put("error", "서버 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * AI 응답을 { result: "..." } Map으로 포장.
     * Ollama 미실행 시 null을 받아 에러 메시지를 반환하도록 처리.
     *
     * @param result Ollama 응답 텍스트 (null이면 Ollama 미실행 상태)
     * @return { "result": "..." } 또는 { "error": "..." } Map
     */
    private Map<String, String> buildAiResponse(String result) {
        Map<String, String> map = new HashMap<>();
        if (result != null) {
            map.put("result", stripMarkdown(result));  // 마크다운 기호 제거 후 반환
        } else {
            log.warn("Ollama AI 응답 없음 (null) - Ollama 실행 여부 확인 필요");
            map.put("error", "AI 응답을 가져오지 못했습니다. Ollama가 실행 중인지 확인해주세요.");
        }
        return map;
    }

    /**
     * AI 응답 텍스트에서 마크다운 기호 제거.
     * LLM이 지시와 달리 마크다운을 포함해 응답할 때를 대비한 후처리.
     *
     * 제거 항목: **볼드**, *이탤릭*, ## 헤더, - 불릿, `인라인코드`
     *
     * @param text 원본 AI 응답 텍스트
     * @return 마크다운 기호가 제거된 순수 텍스트
     */
    private String stripMarkdown(String text) {
        if (text == null) return null;
        return text
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")   // **bold** → bold
                .replaceAll("\\*(.+?)\\*", "$1")           // *italic* → italic
                .replaceAll("(?m)^#{1,6}\\s*", "")        // ## 헤더 제거
                .replaceAll("(?m)^\\s*[-*+]\\s+", "- ")   // 불릿 기호 통일
                .replaceAll("`(.+?)`", "$1")               // `code` → code
                .trim();
    }
}
