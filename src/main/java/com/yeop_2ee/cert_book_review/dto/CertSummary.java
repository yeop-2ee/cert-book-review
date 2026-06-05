package com.yeop_2ee.cert_book_review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 대시보드 자격증별 요약 카드 데이터를 담는 DTO.
 *
 * 사용 위치: ReviewController.index() → index.mustache의 자격증별 요약 테이블
 * 생성 방식: 자격증 이름으로 그룹핑 후 통계 계산
 */
@AllArgsConstructor
@Getter
@ToString
public class CertSummary {

    private String certName;      // 자격증 이름 (그룹 키)
    private int totalCount;       // 해당 자격증 전체 리뷰 수
    private int passRateNum;      // 합격률 숫자 (progress bar width 계산용)
    private String avgDifficulty; // 평균 체감 난이도 ("하"/"중"/"상" 중 하나)
    private String topBook;       // 리뷰가 가장 많은 교재명 (가장 인기 있는 교재)

    /**
     * Mustache 조건부 렌더링용 헬퍼 메서드.
     * Review 엔티티와 동일한 이유로 Mustache {{#diffLow}} 블록 처리에 사용.
     */
    public boolean getDiffLow()  { return "하".equals(avgDifficulty); }
    public boolean getDiffMid()  { return "중".equals(avgDifficulty); }
    public boolean getDiffHigh() { return "상".equals(avgDifficulty); }
}
