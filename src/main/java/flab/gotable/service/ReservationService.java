package flab.gotable.service;

import flab.gotable.dto.request.ReservationRequestDto;
import flab.gotable.dto.response.ReservationResponseDto;
import flab.gotable.exception.*;
import flab.gotable.mapper.MemberMapper;
import flab.gotable.mapper.ReservationMapper;
import flab.gotable.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationBusinessService reservationBusinessService;
    private final ReservationMapper reservationMapper;
    private final MemberMapper memberMapper;
    private final StoreMapper storeMapper;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponseDto reservePessimisticLock(ReservationRequestDto reservationRequestDto) {

        validateReservationRequest(reservationRequestDto);

        // 락 획득 여부
        if(reservationMapper.getRestaurantLock(reservationRequestDto.getRestaurantId()) <= 0) {
            // 락 획득 실패
            throw new LockFailureException(ErrorCode.LOCK_ACQUISITION_FAILED, ErrorCode.LOCK_ACQUISITION_FAILED.getMessage());
        }

        return reservationBusinessService.executePessimisticLockReservation(reservationRequestDto);
    }

    @Transactional
    public ReservationResponseDto reserveNamedLock(ReservationRequestDto reservationRequestDto) {

        validateReservationRequest(reservationRequestDto);

        String lockName = "reservation_" + reservationRequestDto.getRestaurantId();
        Integer lockResult = reservationMapper.getNamedLock(lockName);

        // 락 획득 중 에러 발생
        if(lockResult == null) {
            throw new LockFailureException(ErrorCode.LOCK_SYSTEM_FAILURE, ErrorCode.LOCK_SYSTEM_FAILURE.getMessage());
        }

        // 락 획득 실패
        if(lockResult == 0) {
            throw new LockFailureException(ErrorCode.LOCK_ACQUISITION_FAILED, ErrorCode.LOCK_ACQUISITION_FAILED.getMessage());
        }

        try {
            return reservationBusinessService.executeNamedLockReservation(reservationRequestDto);
        } finally {
            Integer releaseResult = reservationMapper.releaseNamedLock(lockName);

            // 락이 존재하지 않는 경우
            if(releaseResult == null) {
                log.warn("Failed to release lock: {}", lockName);
            }
        }
    }

    private void validateReservationRequest(ReservationRequestDto reservationRequestDto) {
        validateMemberExists(reservationRequestDto.getMemberSeq());
        validateRestaurantExists(reservationRequestDto.getRestaurantId());
        checkMemberCount(reservationRequestDto.getRestaurantId(), reservationRequestDto.getMemberCount());
        checkReservationTime(reservationRequestDto.getReservationStartTime(), reservationRequestDto.getReservationEndTime());
    }

    private boolean isMemberExists(long memberSeq) {
        return memberMapper.isMemberExistSeq(memberSeq);
    }

    private boolean isRestaurantExists(long restaurantId) {
        return storeMapper.isRestaurantExistId(restaurantId);
    }

    private void validateMemberExists(long memberSeq) {
        if(!isMemberExists(memberSeq)) {
            throw new MemberNotFoundException(ErrorCode.MEMBER_NOT_FOUND_SEQ, ErrorCode.MEMBER_NOT_FOUND_SEQ.getMessage());
        }
    }

    private void validateRestaurantExists(long restaurantId) {
        if(!isRestaurantExists(restaurantId)) {
            throw new StoreNotFoundException(ErrorCode.STORE_NOT_FOUND, ErrorCode.STORE_NOT_FOUND.getMessage());
        }
    }

    private void checkMemberCount(long restaurantId, long memberCount) {
        // 예약 인원 수가 0 이하일 경우
        if(memberCount <= 0) {
            throw new InvalidReservationMemberCountException(ErrorCode.INVALID_MAX_MEMBER_COUNT, ErrorCode.INVALID_MAX_MEMBER_COUNT.getMessage());
        }

        // 예약 인원 수가 예약 가능 최대 인원 수를 초과했을 경우
        if(memberCount > storeMapper.getMaxMemberCount(restaurantId)) {
            throw new InvalidReservationMemberCountException(ErrorCode.EXCEEDS_MAX_MEMBER_COUNT, ErrorCode.EXCEEDS_MAX_MEMBER_COUNT.getMessage());
        }
    }

    private void checkReservationTime(LocalDateTime startTime, LocalDateTime endTime) {
        // 예약 종료 시간이 예약 시작 시간보다 앞서거나 동일한 경우
        if(endTime.isBefore(startTime) || endTime.equals(startTime)) {
            throw new InvalidReservationTimeException(ErrorCode.INVALID_RESERVATION_TIME, ErrorCode.INVALID_RESERVATION_TIME.getMessage());
        }

        // 예약 시작 시간이 현재 시간보다 이전인 경우
        if(startTime.isBefore(LocalDateTime.now())) {
            throw new InvalidReservationTimeException(ErrorCode.PAST_RESERVATION_TIME, ErrorCode.PAST_RESERVATION_TIME.getMessage());
        }
    }
}
