package org.sopt.catchtable;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CatchtableApplication {

	// 명세·시드 데이터가 KST 기준이므로 시간 저장 기준을 KST로 통일한다
	// Hibernate·DB 커넥션이 시작 시점의 시간대를 쓰므로, 스프링이 뜨기 전에 고정해야 한다
	public static final String TIME_ZONE = "Asia/Seoul";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(TIME_ZONE));
		SpringApplication.run(CatchtableApplication.class, args);
	}

}
