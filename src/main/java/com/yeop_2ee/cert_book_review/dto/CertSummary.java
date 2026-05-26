package com.yeop_2ee.cert_book_review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class CertSummary {

    private String certName;      // 자격증 이름
    private int totalCount;       // 전체 리뷰 수
    private int passedCount;      // 합격자 수
    private String passRate;      // 합격률 문자열 (예: "75%")
    private int passRateNum;      // 합격률 숫자 (progress bar용)
    private String passRateClass; // Bootstrap 색상 클래스 (success/warning/danger)
    private String avgDifficulty; // 평균 난이도 (하/중/상)
    private String diffClass;     // 난이도 Bootstrap 색상 클래스
    private String topBook;       // 합격자들이 가장 많이 사용한 교재

    public boolean getDiffLow()  { return "하".equals(avgDifficulty); }
    public boolean getDiffMid()  { return "중".equals(avgDifficulty); }
    public boolean getDiffHigh() { return "상".equals(avgDifficulty); }
}
