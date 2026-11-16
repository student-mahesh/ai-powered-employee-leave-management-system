package com.mahesh.empleave.ai;

import com.mahesh.empleave.holiday.HolidayRepository;
import com.mahesh.empleave.leave.LeaveService;
import com.mahesh.empleave.leavebalance.LeaveBalanceRepository;
import com.mahesh.empleave.user.UserRepository;
import java.time.LocalDate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

@RequiredArgsConstructor
@Slf4j
public class AiTools {
    private final String email;
    private final UserRepository users;
    private final LeaveBalanceRepository balances;
    private final HolidayRepository holidays;
    private final LeaveService leaves;

    @Tool(description = "Read the authenticated user's current leave balances")
    public String leaveBalance() {
        return timed("leaveBalance", () -> {
            var user = users.findByEmail(email).orElseThrow();
            return balances.findByUser(user).stream().map(item -> item.getLeaveType().getName() + ": " + item.getRemainingDays()).collect(Collectors.joining(", "));
        });
    }

    @Tool(description = "Read the authenticated user's recent leave request status")
    public String leaveHistory() {
        return timed("leaveHistory", () -> {
            var user = users.findByEmail(email).orElseThrow();
            return leaves.historyFor(user).stream().limit(20).map(item -> item.getLeaveType() + " " + item.getStartDate() + " to " + item.getEndDate() + " (" + item.getStatus() + ")").collect(Collectors.joining("; "));
        });
    }

    @Tool(description = "Look up company holidays between two ISO dates")
    public String companyHolidays(String from, String to) {
        return timed("companyHolidays", () -> holidays.findByHolidayDateBetween(LocalDate.parse(from), LocalDate.parse(to)).stream().map(item -> item.getHolidayDate() + ": " + item.getName()).collect(Collectors.joining(", ")));
    }

    public String applyLeave(String leaveType, String startDate, String endDate, String reason, boolean confirmed) {
        return "Leave submission is available only through the conversation-bound YES/NO confirmation workflow.";
    }

    private String timed(String operation, java.util.function.Supplier<String> action) {
        var started = System.nanoTime();
        try { return action.get(); }
        finally { log.info("ai_chat_tool_execution tool={} durationMs={}", operation, (System.nanoTime() - started) / 1_000_000); }
    }
}
