package com.yeop_2ee.cert_book_review.service;

import com.yeop_2ee.cert_book_review.dto.ReviewForm;
import com.yeop_2ee.cert_book_review.entity.Review;
import com.yeop_2ee.cert_book_review.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

/**
 * 리뷰 비즈니스 로직 서비스.
 *
 * 컨트롤러(HTTP 요청/응답 처리)와 리포지토리(DB 접근) 사이의 중간 계층.
 * 핵심 규칙: 데이터를 변경하는 메서드(생성·수정·삭제)는 반드시 @Transactional 적용.
 *   → 도중에 예외 발생 시 변경 사항이 자동으로 롤백되어 데이터 정합성 보장.
 *
 * 계층 구조: Controller → Service → Repository → DB
 *
 * @Service    : Spring이 이 클래스를 서비스 빈으로 등록하고 @Autowired 주입 대상으로 인식
 * @Slf4j      : log.info / log.error 등 로그 메서드 사용 가능
 * @Transactional : 클래스 레벨 기본 설정 (readOnly=false), 읽기 메서드는 아래에서 readOnly=true 오버라이드
 */
@Slf4j
@Service
@Transactional
public class ReviewService {

    /** DB 접근을 위한 리포지토리 (Spring이 자동으로 주입) */
    @Autowired
    private ReviewRepository reviewRepository;

    /**
     * 전체 리뷰 목록 조회.
     *
     * @Transactional(readOnly = true): 읽기 전용 트랜잭션 → DB 성능 최적화
     * (변경 감지(Dirty Checking) 생략, 읽기 잠금 없음)
     *
     * @return 전체 리뷰 ArrayList
     */
    @Transactional(readOnly = true)
    public ArrayList<Review> findAll() {
        log.info("전체 리뷰 조회 요청");
        return reviewRepository.findAll();
    }

    /**
     * ID로 단건 리뷰 조회.
     *
     * @param id 조회할 리뷰 ID
     * @return Optional<Review> (존재하지 않으면 Optional.empty())
     */
    @Transactional(readOnly = true)
    public Optional<Review> findById(Long id) {
        log.info("리뷰 단건 조회 - id: {}", id);
        return reviewRepository.findById(id);
    }

    /**
     * 새 리뷰 저장 (CREATE).
     *
     * DTO(ReviewForm)를 엔티티(Review)로 변환해 DB에 INSERT.
     * @Transactional: 저장 도중 예외 발생 시 롤백 보장.
     *
     * @param form 클라이언트가 제출한 리뷰 폼 데이터
     * @return 저장된 Review 엔티티 (DB가 부여한 id 포함)
     */
    public Review create(ReviewForm form) {
        Review review = form.toEntity();  // DTO → Entity 변환
        Review saved = reviewRepository.save(review);  // DB INSERT
        log.info("리뷰 저장 완료 - id: {}, certName: {}", saved.getId(), saved.getCertName());
        return saved;
    }

    /**
     * 기존 리뷰 수정 (UPDATE).
     *
     * form의 id와 일치하는 기존 리뷰가 있을 때만 UPDATE 실행.
     * JPA: id가 있는 엔티티를 save()하면 INSERT 대신 UPDATE 실행.
     * @Transactional: 수정 도중 예외 발생 시 롤백 보장.
     *
     * @param form 수정할 리뷰 데이터 (id 필드 포함)
     * @return 수정된 Review 엔티티, 대상 리뷰가 없으면 null
     */
    public Review update(ReviewForm form) {
        Review target = reviewRepository.findById(form.getId()).orElse(null);
        if (target == null) {
            log.warn("수정 대상 리뷰 없음 - id: {}", form.getId());
            return null;
        }
        Review updated = reviewRepository.save(form.toEntity());  // DB UPDATE
        log.info("리뷰 수정 완료 - id: {}", updated.getId());
        return updated;
    }

    /**
     * 리뷰 삭제 (DELETE).
     *
     * id로 리뷰를 찾아 DB에서 삭제.
     * @Transactional: 삭제 도중 예외 발생 시 롤백 보장.
     *
     * @param id 삭제할 리뷰 ID
     */
    public void delete(Long id) {
        Review target = reviewRepository.findById(id).orElse(null);
        if (target == null) {
            log.warn("삭제 대상 리뷰 없음 - id: {}", id);
            return;
        }
        reviewRepository.delete(target);  // DB DELETE
        log.info("리뷰 삭제 완료 - id: {}", id);
    }
}
