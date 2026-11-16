package com.mahesh.empleave.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.mahesh.empleave.holiday.HolidayRepository;
import com.mahesh.empleave.holiday.Holiday;
import com.mahesh.empleave.leave.LeaveService;
import com.mahesh.empleave.leavebalance.LeaveBalanceRepository;
import com.mahesh.empleave.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import java.time.LocalDate;
import java.util.List;

class AiToolsTest {
    @Test
    void leaveApplicationCannotBypassConversationBoundConfirmation() throws Exception {
        var leaves = mock(LeaveService.class);
        var tools = new AiTools("employee@example.com", mock(UserRepository.class), mock(LeaveBalanceRepository.class), mock(HolidayRepository.class), leaves);

        var result = tools.applyLeave("Annual", "2030-01-02", "2030-01-03", "Travel", true);

        assertTrue(result.contains("conversation-bound YES/NO"));
        verifyNoInteractions(leaves);
        assertTrue(!AiTools.class.getMethod("applyLeave", String.class, String.class, String.class, String.class, boolean.class)
                .isAnnotationPresent(Tool.class));
    }

    @Test
    void companyHolidaysReturnsOnlyRepositoryRecordsInRequestedDateRange() {
        var holidays = mock(HolidayRepository.class);
        var from = LocalDate.of(2030, 1, 1);
        var to = LocalDate.of(2030, 12, 31);
        when(holidays.findByHolidayDateBetween(from, to)).thenReturn(List.of(
                new Holiday(LocalDate.of(2030, 1, 26), "Configured Day")));
        var tools = new AiTools("employee@example.com", mock(UserRepository.class), mock(LeaveBalanceRepository.class), holidays, mock(LeaveService.class));

        var result = tools.companyHolidays(from.toString(), to.toString());

        assertEquals("2030-01-26: Configured Day", result);
        verify(holidays).findByHolidayDateBetween(from, to);
    }
}
