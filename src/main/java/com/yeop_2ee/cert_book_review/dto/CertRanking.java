package com.yeop_2ee.cert_book_review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class CertRanking {

    private String certName;          // 자격증 이름
    private List<BookDetail> books;   // 해당 자격증의 교재 순위 목록
}
