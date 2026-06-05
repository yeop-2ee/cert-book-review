package com.yeop_2ee.cert_book_review.repository;

import com.yeop_2ee.cert_book_review.entity.Review;
import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;

/**
 * 리뷰 데이터 접근 인터페이스 (DAO 레이어).
 *
 * Spring Data JPA의 CrudRepository를 상속하면
 * 아래 CRUD 메서드가 구현 코드 없이 자동으로 제공됨:
 *  - save(entity)         : INSERT 또는 UPDATE
 *  - findById(id)         : id로 단건 조회 → Optional<Review> 반환
 *  - findAll()            : 전체 조회 (아래에서 반환 타입을 ArrayList로 오버라이드)
 *  - delete(entity)       : 삭제
 *  - count()              : 총 개수
 *
 * CrudRepository<Review, Long>의 제네릭 인수:
 *  - Review : 대상 엔티티 타입
 *  - Long   : Primary Key 타입 (Review.id)
 */
public interface ReviewRepository extends CrudRepository<Review, Long> {

    /**
     * 전체 리뷰 목록을 ArrayList로 반환하도록 반환 타입 오버라이드.
     * 기본 반환 타입은 Iterable<Review>이나,
     * 컨트롤러에서 인덱스 접근(get(i)) 및 size() 등을 편하게 쓰기 위해 ArrayList로 변경.
     */
    @Override
    ArrayList<Review> findAll();
}
