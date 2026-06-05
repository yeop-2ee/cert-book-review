package com.yeop_2ee.cert_book_review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 자격증 + 해당 자격증의 교재 순위 목록을 묶은 DTO.
 *
 * 사용 위치: ReviewController.ranking() → ranking.mustache
 * 구조: 자격증 1개당 CertRanking 1개, 그 안에 교재 목록(BookDetail)이 순위순으로 들어감
 *
 * 예시:
 *  CertRanking("정보처리기사", [
 *    BookDetail(1위, "시나공", 합격 20명, ...),
 *    BookDetail(2위, "수제비",  합격 15명, ...),
 *    ...
 *  ])
 */
@AllArgsConstructor
@Getter
public class CertRanking {

    private String certName;          // 자격증 이름 (그룹 키)
    private List<BookDetail> books;   // 해당 자격증의 교재 순위 목록 (합격자 수 내림차순 정렬됨)
}
