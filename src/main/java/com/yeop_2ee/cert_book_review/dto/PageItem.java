package com.yeop_2ee.cert_book_review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PageItem {

    private int pageNum;   // 페이지 번호
    private boolean active; // 현재 페이지 여부
}
