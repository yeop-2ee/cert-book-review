package com.yeop_2ee.cert_book_review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class BookRecommend {

    private String bookTitle;      // 교재명
    private int totalCount;        // 총 리뷰 수
    private int passedCount;       // 합격 리뷰 수
    private String passRate;       // "75%"
    private int passRateNum;       // 75 (프로그레스 바용)
    private String avgStudyPeriod; // 평균 공부 기간
}
