package com.yeop_2ee.cert_book_review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class BookRank {

    private int rank;          // 순위
    private String certName;   // 자격증 이름 (TOP5 표시용)
    private String bookTitle;  // 교재 이름
    private int count;         // 합격자 사용 수
}
