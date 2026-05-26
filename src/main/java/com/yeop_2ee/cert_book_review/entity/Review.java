package com.yeop_2ee.cert_book_review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Entity
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String certName;    // 자격증 이름

    @Column
    private String bookTitle;   // 교재 이름

    @Column
    private String difficulty;  // 난이도 (하 / 중 / 상)

    @Column
    private String passed;      // 합격 여부 (Y / N)

    @Column
    private String studyPeriod; // 공부 기간

    @Column
    private String content;     // 리뷰 내용

    @Column
    private String pin;         // 수정/삭제용 4자리 PIN

    // Mustache 조건부 렌더링용 헬퍼 메서드
    public boolean getDiffLow()  { return "하".equals(difficulty); }
    public boolean getDiffMid()  { return "중".equals(difficulty); }
    public boolean getDiffHigh() { return "상".equals(difficulty); }
    public boolean getPassedY()  { return "Y".equals(passed); }
    public boolean getPassedN()  { return "N".equals(passed); }
}
