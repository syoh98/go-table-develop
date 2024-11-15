package flab.gotable.service;

import flab.gotable.domain.entity.DailySchedule;
import flab.gotable.domain.entity.DayInfo;
import flab.gotable.domain.entity.SpecificSchedule;
import flab.gotable.domain.entity.Store;
import flab.gotable.dto.response.StoreDetailsResponseDto;
import flab.gotable.mapper.StoreMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StoreServiceTest {

    private StoreService storeService;
    private final LocalDate currentDate = LocalDate.now().plusDays(7);

    @BeforeEach
    void setup() {
        storeService = new StoreService(new StoreMapper() {
            @Override
            public Store findStoreById(Long id) {
                if (id == 1L) {
                    Store store = new Store();
                    store.setId(1L);
                    store.setName("차알 엘지아트센터 서울점");
                    store.setAddress("서울 강서구 마곡중앙로 136 지하1층");
                    store.setMaxMemberCount(8);
                    store.setMaxAvailableDay(7);
                    return store;
                }
                return null;
            }

            @Override
            public List<DailySchedule> findDailyScheduleByStoreId(Long id) {
                if (id == 1L) {
                    return Arrays.asList(
                            new DailySchedule(DayOfWeek.MONDAY, LocalTime.parse("09:00"), LocalTime.parse("18:00"), 60L),
                            new DailySchedule(DayOfWeek.TUESDAY, LocalTime.parse("09:00"), LocalTime.parse("18:00"), 60L)
                    );
                }
                return Collections.emptyList();
            }

            @Override
            public List<SpecificSchedule> findSpecificScheduleByStoreId(Long id) {
                if (id == 1L) {
                    return Arrays.asList(
                            new SpecificSchedule(currentDate, LocalTime.parse("10:00"), LocalTime.parse("15:00"), 60L)
                    );
                }
                return Collections.emptyList();
            }

            @Override
            public boolean isRestaurantExistId(long restaurantId) {
                return false;
            }

            @Override
            public long getMaxMemberCount(long restaurantId) {
                return 0;
            }
        });
    }

    @Test
    @DisplayName("해당 id를 갖고 있는 식당이 존재하는 경우 TRUE를 반환한다.")
    void isExistStore() {
        // given
        Long storeId = 1L;

        // when
        boolean result = storeService.existById(storeId);

        // then
        Assertions.assertTrue(result);
    }

    @Test
    @DisplayName("해당 id를 갖고 있는 식당이 존재하지 않을 경우 FALSE를 반환한다.")
    void isNotExistStore() {
        // given
        Long storeId = 2L;

        // when
        boolean result = storeService.existById(storeId);

        // then
        Assertions.assertFalse(result);
    }


    @Test
    @DisplayName("식당 id로 식당 상세 정보 조회에 성공한다.")
    void getStoreDetailSuccess() {
        // given
        Long storeId = 1L;

        // when
        StoreDetailsResponseDto result = storeService.getStoreDetail(storeId);

        // then
        Assertions.assertNotNull(result);

        Assertions.assertEquals(storeId, result.getId());
        Assertions.assertEquals("차알 엘지아트센터 서울점", result.getName());
        Assertions.assertEquals("서울 강서구 마곡중앙로 136 지하1층", result.getAddress());
        Assertions.assertEquals(8, result.getMaxMemberCount());
        Assertions.assertEquals(7, result.getMaxAvailableDay());

        String expectedOpenSchedule = "MONDAY 09:00 ~ 18:00, TUESDAY 09:00 ~ 18:00";
        Assertions.assertEquals(expectedOpenSchedule, result.getOpenSchedule());

        Map<String, DayInfo> availableDays = result.getAvailableDays();
        String expectedDate = currentDate.toString();
        Assertions.assertNotNull(availableDays);
        Assertions.assertTrue(availableDays.containsKey(expectedDate));

        DayInfo dayInfo = availableDays.get(expectedDate);
        Assertions.assertNotNull(dayInfo);

        List<String> selectableTimes = dayInfo.getSelectableTimes();
        Assertions.assertNotNull(selectableTimes);
        Assertions.assertEquals(Arrays.asList("10:00", "11:00", "12:00", "13:00", "14:00"), selectableTimes);
    }
}
