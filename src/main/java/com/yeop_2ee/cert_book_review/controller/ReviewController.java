package com.yeop_2ee.cert_book_review.controller;

import com.yeop_2ee.cert_book_review.dto.BookDetail;
import com.yeop_2ee.cert_book_review.dto.BookRank;
import com.yeop_2ee.cert_book_review.dto.CertRanking;
import com.yeop_2ee.cert_book_review.dto.CertSummary;
import com.yeop_2ee.cert_book_review.dto.PageItem;
import com.yeop_2ee.cert_book_review.dto.ReviewForm;
import com.yeop_2ee.cert_book_review.entity.Review;
import com.yeop_2ee.cert_book_review.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Slf4j
@Controller
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    private static final int PAGE_SIZE = 10; // 한 페이지당 리뷰 수

    private static final int CERT_PAGE_SIZE = 5;

    @GetMapping("/reviews")
    public String index(Model model) {
        ArrayList<Review> allReviews = reviewRepository.findAll();

        // ── 1. 전체 기준 통계 ───────────────────────────────
        int total = allReviews.size();
        int passedCount = 0;
        List<String> uniqueCerts = new ArrayList<>();
        for (Review r : allReviews) {
            if ("Y".equals(r.getPassed())) passedCount++;
            if (!uniqueCerts.contains(r.getCertName())) uniqueCerts.add(r.getCertName());
        }
        int passRateNum = total > 0 ? (passedCount * 100 / total) : 0;

        // ── 2. 교재 랭킹 TOP 5 ──────────────────────────────
        Map<String, int[]> top5Map = new LinkedHashMap<>();
        for (Review r : allReviews) {
            if ("Y".equals(r.getPassed())) {
                String key = r.getCertName() + "|" + r.getBookTitle();
                if (!top5Map.containsKey(key)) top5Map.put(key, new int[]{0});
                top5Map.get(key)[0]++;
            }
        }
        List<BookRank> rankList = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : top5Map.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            rankList.add(new BookRank(0, parts[0], parts[1], entry.getValue()[0]));
        }
        rankList.sort((a, b) -> b.getCount() - a.getCount());
        for (int i = 0; i < rankList.size(); i++) {
            BookRank r = rankList.get(i);
            rankList.set(i, new BookRank(i + 1, r.getCertName(), r.getBookTitle(), r.getCount()));
        }
        List<BookRank> top5RankList = rankList.size() > 5 ? rankList.subList(0, 5) : rankList;

        // ── 3. 최신 합격 후기 TOP 5 ────────────────────────
        List<Review> recentPassedList = new ArrayList<>();
        for (int i = allReviews.size() - 1; i >= 0; i--) {
            if ("Y".equals(allReviews.get(i).getPassed())) {
                recentPassedList.add(allReviews.get(i));
                if (recentPassedList.size() == 5) break;
            }
        }

        // ── 4. 자격증별 요약 ───────────────────────────────
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
                if ("하".equals(r.getDifficulty())) diffSum += 1;
                else if ("중".equals(r.getDifficulty())) diffSum += 2;
                else if ("상".equals(r.getDifficulty())) diffSum += 3;
                bookCount.put(r.getBookTitle(), bookCount.getOrDefault(r.getBookTitle(), 0) + 1);
            }
            int certPassRateNum = certTotal > 0 ? (certPassed * 100 / certTotal) : 0;
            double avgDiff = (double) diffSum / certTotal;
            String avgDifficulty = avgDiff <= 1.5 ? "하" : avgDiff <= 2.5 ? "중" : "상";
            String topBook = "-";
            int maxCnt = 0;
            for (Map.Entry<String, Integer> b : bookCount.entrySet()) {
                if (b.getValue() > maxCnt) { maxCnt = b.getValue(); topBook = b.getKey(); }
            }
            certSummaryList.add(new CertSummary(
                    certName, certTotal, certPassed,
                    certPassRateNum + "%", certPassRateNum, "",
                    avgDifficulty, "", topBook));
        }

        // ── 5. 모델에 등록 (필터링·페이지네이션은 JS 처리) ──
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
        model.addAttribute("reviewList", allReviews);

        return "reviews/index";
    }

    @GetMapping("/reviews/index")
    public String indexAlias() {
        return "redirect:/reviews";
    }

    @GetMapping("/reviews/new")
    public String newReview() {
        return "reviews/new";
    }

    @PostMapping("/reviews/create")
    public String create(ReviewForm form) {
        log.info(form.toString());
        Review review = form.toEntity();
        Review saved = reviewRepository.save(review);
        return "redirect:/reviews/" + saved.getId();
    }

    @GetMapping("/reviews/{id}")
    public String show(@PathVariable Long id, Model model) {
        Review review = reviewRepository.findById(id).orElse(null);
        model.addAttribute("review", review);
        return "reviews/show";
    }

    @GetMapping("/reviews/{id}/pin-check")
    public String pinCheckForm(@PathVariable Long id, @RequestParam String action, Model model) {
        model.addAttribute("id", id);
        model.addAttribute("action", action);
        return "reviews/pin-check";
    }

    @PostMapping("/reviews/{id}/pin-check")
    public String pinCheck(@PathVariable Long id,
                           @RequestParam String action,
                           @RequestParam String pin,
                           RedirectAttributes rttr) {
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) return "redirect:/reviews";

        if (!review.getPin().equals(pin)) {
            rttr.addFlashAttribute("errorMsg", "PIN이 올바르지 않습니다.");
            return "redirect:/reviews/" + id + "/pin-check?action=" + action;
        }

        if ("delete".equals(action)) {
            reviewRepository.delete(review);
            rttr.addFlashAttribute("msg", "리뷰가 삭제되었습니다!");
            return "redirect:/reviews";
        }

        return "redirect:/reviews/" + id + "/edit";
    }

    @GetMapping("/reviews/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Review review = reviewRepository.findById(id).orElse(null);
        model.addAttribute("review", review);
        return "reviews/edit";
    }

    @PostMapping("/reviews/update")
    public String update(ReviewForm form) {
        log.info(form.toString());
        Review reviewEntity = form.toEntity();
        Review target = reviewRepository.findById(reviewEntity.getId()).orElse(null);
        if (target != null) reviewRepository.save(reviewEntity);
        return "redirect:/reviews/" + reviewEntity.getId();
    }

    @GetMapping("/reviews/stats")
    public String stats(@RequestParam(defaultValue = "") String certName, Model model) {
        ArrayList<Review> allReviews = reviewRepository.findAll();

        // 드롭다운용 자격증 목록
        List<String> certNames = new ArrayList<>();
        for (Review r : allReviews) {
            if (!certNames.contains(r.getCertName())) certNames.add(r.getCertName());
        }

        // 선택 자격증으로 필터 (미선택 시 전체)
        List<Review> filtered = new ArrayList<>();
        for (Review r : allReviews) {
            if (certName.isEmpty() || certName.equals(r.getCertName())) filtered.add(r);
        }

        // 난이도 분포
        int diffLow = 0, diffMid = 0, diffHigh = 0;
        for (Review r : filtered) {
            if ("하".equals(r.getDifficulty())) diffLow++;
            else if ("중".equals(r.getDifficulty())) diffMid++;
            else if ("상".equals(r.getDifficulty())) diffHigh++;
        }

        // 공부 기간별 합격률
        int[] pCount = {0, 0, 0, 0};
        int[] pPassed = {0, 0, 0, 0};
        for (Review r : filtered) {
            int months = parseMonths(r.getStudyPeriod());
            int g = months < 1 ? 0 : months <= 3 ? 1 : months <= 6 ? 2 : 3;
            pCount[g]++;
            if ("Y".equals(r.getPassed())) pPassed[g]++;
        }

        model.addAttribute("certName", certName);
        model.addAttribute("certNames", certNames);
        model.addAttribute("totalReviews", filtered.size());
        model.addAttribute("totalReviewsFmt", String.format("%,d", filtered.size()));
        int diffTotal = diffLow + diffMid + diffHigh;
        model.addAttribute("diffLow", diffLow);
        model.addAttribute("diffMid", diffMid);
        model.addAttribute("diffHigh", diffHigh);
        model.addAttribute("diffLowPct",  diffTotal > 0 ? Math.round(diffLow  * 100.0 / diffTotal) : 0);
        model.addAttribute("diffMidPct",  diffTotal > 0 ? Math.round(diffMid  * 100.0 / diffTotal) : 0);
        model.addAttribute("diffHighPct", diffTotal > 0 ? Math.round(diffHigh * 100.0 / diffTotal) : 0);
        // 공부 기간별 통계 (Chart.js에 개별 전달)
        model.addAttribute("pr0", pCount[0] > 0 ? pPassed[0] * 100 / pCount[0] : 0);
        model.addAttribute("pr1", pCount[1] > 0 ? pPassed[1] * 100 / pCount[1] : 0);
        model.addAttribute("pr2", pCount[2] > 0 ? pPassed[2] * 100 / pCount[2] : 0);
        model.addAttribute("pr3", pCount[3] > 0 ? pPassed[3] * 100 / pCount[3] : 0);
        model.addAttribute("pc0", pCount[0]);
        model.addAttribute("pc1", pCount[1]);
        model.addAttribute("pc2", pCount[2]);
        model.addAttribute("pc3", pCount[3]);
        model.addAttribute("hasData", !filtered.isEmpty());
        return "reviews/stats";
    }

    @GetMapping("/reviews/recommend")
    public String recommend(@RequestParam(defaultValue = "") String certName) {
        if (certName.isEmpty()) return "redirect:/reviews/ranking";
        return "redirect:/reviews/ranking?certName=" + certName;
    }

    // 공부 기간 텍스트 → 개월 수 변환
    private int parseMonths(String period) {
        if (period == null || period.isEmpty()) return 0;
        try {
            String num = period.replaceAll("[^0-9]", "");
            if (num.isEmpty()) return 0;
            int n = Integer.parseInt(num);
            if (period.contains("년")) return n * 12;
            if (period.contains("개월") || period.contains("달")) return n;
            if (period.contains("주")) return Math.max(1, n / 4);
            if (period.contains("일")) return Math.max(1, n / 30);
        } catch (Exception e) { /* ignore */ }
        return 0;
    }

    @GetMapping("/reviews/ranking")
    public String ranking(Model model) {
        ArrayList<Review> reviewList = reviewRepository.findAll();

        // 자격증 → 교재 → [총리뷰, 합격수, 총기간(개월), 기간있는리뷰수]
        Map<String, Map<String, int[]>> certBookMap = new LinkedHashMap<>();
        for (Review r : reviewList) {
            String cert = r.getCertName();
            String book = r.getBookTitle();
            if (!certBookMap.containsKey(cert)) certBookMap.put(cert, new LinkedHashMap<>());
            Map<String, int[]> bookMap = certBookMap.get(cert);
            if (!bookMap.containsKey(book)) bookMap.put(book, new int[]{0, 0, 0, 0});
            int[] s = bookMap.get(book);
            s[0]++;
            if ("Y".equals(r.getPassed())) s[1]++;
            int months = parseMonths(r.getStudyPeriod());
            if (months > 0) { s[2] += months; s[3]++; }
        }

        // 자격증별 교재 순위 목록 생성 (합격자 수 기준 정렬)
        List<CertRanking> certRankingList = new ArrayList<>();
        for (Map.Entry<String, Map<String, int[]>> certEntry : certBookMap.entrySet()) {
            List<BookDetail> books = new ArrayList<>();
            for (Map.Entry<String, int[]> bookEntry : certEntry.getValue().entrySet()) {
                int[] s = bookEntry.getValue();
                int rateNum = s[0] > 0 ? s[1] * 100 / s[0] : 0;
                String avgPeriod = s[3] > 0 ? (s[2] / s[3]) + "개월" : "-";
                books.add(new BookDetail(0, bookEntry.getKey(), s[1], s[0], rateNum + "%", rateNum, avgPeriod));
            }
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
}
