package com.yeop_2ee.cert_book_review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 자격증 교재 리뷰 서비스의 Spring Boot 애플리케이션 진입점.
 *
 * @SpringBootApplication 이 붙으면 다음 3가지가 자동 활성화됨:
 *  - @Configuration   : 이 클래스 자체를 스프링 설정 클래스로 인식
 *  - @EnableAutoConfiguration : classpath 기반으로 빈(Bean)·설정 자동 구성 (내장 톰캣 포함)
 *  - @ComponentScan   : 현재 패키지 하위의 @Controller, @Service, @Repository 등을 자동 등록
 */
@SpringBootApplication
public class CertBookReviewApplication {

	/**
	 * 애플리케이션 시작점.
	 * SpringApplication.run()이 내장 톰캣 서버를 기동하고
	 * application.properties에 지정된 포트(기본 8080)로 요청을 받기 시작함.
	 */
	public static void main(String[] args) {
		SpringApplication.run(CertBookReviewApplication.class, args);
	}

}
