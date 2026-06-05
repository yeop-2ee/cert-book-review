package com.yeop_2ee.cert_book_review.dto;

import com.yeop_2ee.cert_book_review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 리뷰 작성·수정 폼 데이터를 담는 DTO (Data Transfer Object).
 *
 * 역할: HTML 폼(new.mustache, edit.mustache)에서 POST로 전송된 데이터를
 *       컨트롤러가 이 객체로 자동 바인딩받아 Review 엔티티로 변환함.
 *
 * DTO를 별도로 분리하는 이유:
 *  - 폼 입력값과 DB 엔티티를 직접 연결하면 보안 문제(Mass Assignment) 발생 가능
 *  - 변환 로직(toEntity)을 한 곳에서 관리하기 위함
 *
 * @Getter : 서비스 계층에서 id 등 필드를 직접 읽을 수 있도록 getter 자동 생성
 */
@AllArgsConstructor
@Getter
@ToString
public class ReviewForm {

    private Long id;           // 리뷰 ID (신규 작성 시 null, 수정 시 기존 ID)
    private String certName;   // 자격증 이름
    private String bookTitle;  // 교재 이름
    private String difficulty; // 체감 난이도 ("하"/"중"/"상")
    private String passed;     // 합격 여부 ("Y"/"N")
    private String studyPeriod;// 공부 기간 텍스트
    private String content;    // 리뷰 본문
    private String pin;        // 수정/삭제용 4자리 PIN

    /**
     * 폼 데이터를 Review 엔티티로 변환.
     * 컨트롤러에서 reviewRepository.save()에 넘기기 전에 호출함.
     */
    public Review toEntity() {
        return new Review(id, certName, bookTitle, difficulty, passed, studyPeriod, content, pin);
    }
}
