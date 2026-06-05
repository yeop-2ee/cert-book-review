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

/**
 * 리뷰 엔티티 - DB의 'review' 테이블과 1:1로 매핑되는 JPA 클래스.
 *
 * Lombok 어노테이션 역할:
 *  - @AllArgsConstructor : 모든 필드를 인자로 받는 생성자 자동 생성
 *  - @NoArgsConstructor  : 인자 없는 기본 생성자 자동 생성 (JPA 필수)
 *  - @ToString           : toString() 메서드 자동 생성 (디버깅 편의)
 *  - @Getter             : 모든 필드의 getter 메서드 자동 생성
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Entity
public class Review {

    /** 리뷰 고유 ID (DB에서 자동 증가, Primary Key) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 자격증 이름 (예: "정보처리기사", "SQLD") */
    @Column
    private String certName;

    /** 사용한 교재 이름 (예: "시나공 정보처리기사 필기") */
    @Column
    private String bookTitle;

    /** 체감 난이도: "하", "중", "상" 세 가지 값만 허용 */
    @Column
    private String difficulty;

    /** 합격 여부: "Y"(합격) 또는 "N"(불합격) */
    @Column
    private String passed;

    /** 공부 기간 텍스트 (예: "3개월", "6개월", "1년") */
    @Column
    private String studyPeriod;

    /** 리뷰 본문 내용 */
    @Column
    private String content;

    /** 수정·삭제 인증용 4자리 숫자 PIN (로그인 없는 간이 인증) */
    @Column
    private String pin;

    /**
     * Mustache 템플릿 조건부 렌더링용 헬퍼 메서드.
     *
     * Mustache는 if-else 문법을 지원하지 않아서,
     * {{#diffLow}}...{{/diffLow}} 처럼 boolean 메서드로 분기 처리함.
     * Java에서 getDiffLow()가 true면 해당 블록이 렌더링됨.
     */
    public boolean getDiffLow()  { return "하".equals(difficulty); }  // 난이도 '하'인 경우 true
    public boolean getDiffMid()  { return "중".equals(difficulty); }  // 난이도 '중'인 경우 true
    public boolean getDiffHigh() { return "상".equals(difficulty); }  // 난이도 '상'인 경우 true
    public boolean getPassedY()  { return "Y".equals(passed); }       // 합격한 경우 true
    public boolean getPassedN()  { return "N".equals(passed); }       // 불합격한 경우 true
}
