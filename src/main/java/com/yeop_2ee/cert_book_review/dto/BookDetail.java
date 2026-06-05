package com.yeop_2ee.cert_book_review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 자격증별 교재 상세 정보를 담는 DTO.
 *
 * 사용 위치: ReviewController.ranking() → ranking.mustache의 교재 순위 테이블
 * 정렬 기준: 합격자 수(passedCount) 내림차순
 */
@AllArgsConstructor
@Getter
public class BookDetail {

    private int rank;              // 해당 자격증 내 교재 순위 (1위가 1위 교재)
    private String bookTitle;      // 교재 이름
    private int passedCount;       // 이 교재 사용자 중 합격자 수
    private int totalCount;        // 이 교재 전체 리뷰 수 (합격+불합격)
    private int passRateNum;       // 합격률 숫자 (progress bar의 width % 계산용)
    private String avgStudyPeriod; // 합격자들의 평균 공부 기간 텍스트 (예: "4개월")

    /**
     * 1위 교재 여부 반환.
     * ranking.mustache에서 1위 교재에 왕관 아이콘 등 특별 표시를 위해 사용.
     */
    public boolean isTop() { return rank == 1; }
}
