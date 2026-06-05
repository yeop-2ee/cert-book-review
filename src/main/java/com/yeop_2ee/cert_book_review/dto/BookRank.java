package com.yeop_2ee.cert_book_review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 대시보드 교재 TOP5 순위 데이터를 담는 DTO.
 *
 * 사용 위치: ReviewController.index() → index.mustache의 TOP5 테이블
 * 정렬 기준: 합격자(passed="Y")가 해당 교재를 사용한 횟수(count) 내림차순
 */
@AllArgsConstructor
@Getter
@ToString
public class BookRank {

    private int rank;          // 전체 순위 (1~5)
    private String certName;   // 자격증 이름 (어느 자격증 교재인지 구분)
    private String bookTitle;  // 교재 이름
    private int count;         // 합격자 중 해당 교재 사용 횟수
}
