package com.yeop_2ee.cert_book_review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BookDetail {

    private int rank;
    private String bookTitle;
    private int passedCount;       // 합격자 수
    private int totalCount;        // 총 리뷰 수
    private String passRate;       // 합격률 문자열
    private int passRateNum;       // 합격률 숫자 (progress bar용)
    private String avgStudyPeriod; // 평균 공부 기간

    public boolean isTop() { return rank == 1; }
}
