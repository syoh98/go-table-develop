package flab.gotable.service;

import flab.gotable.dto.request.ReservationRequestDto;
import flab.gotable.mapper.ReservationMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Slf4j
class ReservationLockServiceTest {

    @Container
    public static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0.33")
            .withInitScript("init.sql");

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", mySQLContainer::getDriverClassName);
    }

    @BeforeAll
    public static void setup() {
        mySQLContainer.start();
    }

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private ReservationService reservationService;

    private final LocalDateTime startTime = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);
    private final LocalDateTime endTime = startTime.plusHours(1);

    @Test
    @DisplayName("동시에 예약을 시도하는 경우 유효한 예약은 1건만 존재한다.")
    void concurrentReservationTest() throws InterruptedException {
        // given
        ReservationRequestDto requestDto = new ReservationRequestDto(1L, 1L, startTime, endTime, 3L);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    reservationService.reserveNamedLock(requestDto);
                    // reservationService.reservePessimisticLock(requestDto);
                } catch (Exception e) {
                    System.err.println("Exception occurred: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        long reservationCount = reservationMapper.getReservationCount(1L, startTime, endTime);

        Assertions.assertEquals(1, reservationCount);
    }

    @Test
    @DisplayName("REQUIRES_NEW 트랜잭션은 메인 트랜잭션이 실패해도 커밋된다.")
    void transactionPropagationRequiresNewTest() {
        // given
        ReservationRequestDto requestDto = new ReservationRequestDto(1L, 1L, startTime, endTime, 3L);

        // when
        try {
            reservationService.reserveNamedLock(requestDto);
            throw new RuntimeException("메인 트랜잭션 실패");
        } catch (RuntimeException e) {
            System.err.println("Main transaction failed: " + e.getMessage());
        }

        // then
        long reservationCount = reservationMapper.getReservationCount(1L, startTime, endTime);

        Assertions.assertEquals(1, reservationCount);
    }
}
