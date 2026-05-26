package com.yeop_2ee.cert_book_review.dto;

import com.yeop_2ee.cert_book_review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class ReviewForm {

    private Long id;
    private String certName;
    private String bookTitle;
    private String difficulty;
    private String passed;
    private String studyPeriod;
    private String content;
    private String pin;

    public Review toEntity() {
        return new Review(id, certName, bookTitle, difficulty, passed, studyPeriod, content, pin);
    }
}
