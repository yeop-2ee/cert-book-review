package com.yeop_2ee.cert_book_review.repository;

import com.yeop_2ee.cert_book_review.entity.Review;
import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;

public interface ReviewRepository extends CrudRepository<Review, Long> {

    @Override
    ArrayList<Review> findAll();
}
