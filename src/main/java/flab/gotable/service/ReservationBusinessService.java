package flab.gotable.service;

import flab.gotable.domain.entity.Reservation;
import flab.gotable.dto.request.ReservationRequestDto;
import flab.gotable.dto.response.ReservationResponseDto;
import flab.gotable.exception.DuplicatedReservationException;
import flab.gotable.exception.ErrorCode;
import flab.gotable.exception.ScheduleNotFoundException;
import flab.gotable.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ReservationBusinessService {

    private final ReservationMapper reservationMapper;

    public ReservationResponseDto executePessimisticLockReservation(ReservationRequestDto reservationRequestDto) {
        return executeReservation(reservationRequestDto);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReservationResponseDto executeNamedLockReservation(ReservationRequestDto reservationRequestDto) {
        return executeReservation(reservationRequestDto);
    }

    private ReservationResponseDto executeReservation(ReservationRequestDto reservationRequestDto) {
        final long restaurantId = reservationRequestDto.getRestaurantId();
        final LocalDateTime reservationStartTime = reservationRequestDto.getReservationStartTime();
        final LocalDateTime reservationEndTime = reservationRequestDto.getReservationEndTime();

        // 예약하려는 시간에 다른 사용자가 예약한 경우
        if (!isReservationAvailable(restaurantId, reservationStartTime, reservationEndTime)) {
            throw new DuplicatedReservationException(ErrorCode.DUPLICATED_RESERVATION_TIME, ErrorCode.DUPLICATED_RESERVATION_TIME.getMessage());
        }

        // 예약하려는 시간이 일반 또는 영업 스케줄 중에 존재하지 않는 경우
        if (!isExistSchedule(restaurantId, reservationStartTime, reservationEndTime)) {
            throw new ScheduleNotFoundException(ErrorCode.RESERVATION_TIME_NOT_FOUND, ErrorCode.RESERVATION_TIME_NOT_FOUND.getMessage());
        }

        Reservation reservation = ReservationRequestDto.toEntity(reservationRequestDto);
        reservationMapper.saveReservation(reservation);

        return new ReservationResponseDto(reservation);
    }

    private boolean isReservationAvailable(long restaurantId, LocalDateTime reservationStartTime, LocalDateTime reservationEndTime) {
        return !reservationMapper.isDuplicatedReservation(restaurantId, reservationStartTime, reservationEndTime);
    }

    private boolean isExistSchedule(long restaurantId, LocalDateTime reservationStartTime, LocalDateTime reservationEndTime) {
        final LocalTime startTime = reservationStartTime.toLocalTime();
        final LocalTime endTime = reservationEndTime.toLocalTime();
        final String dayOfWeek = reservationStartTime.getDayOfWeek().toString();

        boolean isExistDailySchedule = reservationMapper.isExistDailySchedule(restaurantId, dayOfWeek, startTime, endTime);
        boolean isExistSpecificSchedule = reservationMapper.isExistSpecificSchedule(restaurantId, reservationStartTime.toLocalDate(), startTime, endTime);

        return isExistDailySchedule || isExistSpecificSchedule;
    }
}
