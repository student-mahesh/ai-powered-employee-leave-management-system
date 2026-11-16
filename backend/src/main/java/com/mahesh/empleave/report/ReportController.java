package com.mahesh.empleave.report;

import com.mahesh.empleave.leave.LeaveApplication;
import com.mahesh.empleave.leave.LeaveRepository;
import com.mahesh.empleave.leave.LeaveStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {
    private final LeaveRepository leaves;

    public record LeaveSummary(
            LocalDate startDate,
            LocalDate endDate,
            long totalRequests,
            long pending,
            long approved,
            long rejected,
            long cancelled,
            long requestedWorkingDays,
            Map<String, Long> byLeaveType) {
    }

    public record LeaveTrend(String month, long requests, long requestedWorkingDays) {
    }

    @GetMapping("/leave-summary")
    LeaveSummary summary(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        var range = range(startDate, endDate);
        var matching = matching(range.start(), range.end());
        return new LeaveSummary(
                range.start(),
                range.end(),
                matching.size(),
                count(matching, LeaveStatus.PENDING),
                count(matching, LeaveStatus.APPROVED),
                count(matching, LeaveStatus.REJECTED),
                count(matching, LeaveStatus.CANCELLED),
                matching.stream().mapToLong(LeaveApplication::getWorkingDays).sum(),
                matching.stream().collect(Collectors.groupingBy(
                        LeaveApplication::getLeaveType,
                        LinkedHashMap::new,
                        Collectors.counting())));
    }

    @GetMapping("/leave-trends")
    List<LeaveTrend> trends(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        var range = range(startDate, endDate);
        return matching(range.start(), range.end()).stream()
                .collect(Collectors.groupingBy(item -> YearMonth.from(item.getStartDate())))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> new LeaveTrend(
                        entry.getKey().toString(),
                        entry.getValue().size(),
                        entry.getValue().stream().mapToLong(LeaveApplication::getWorkingDays).sum()))
                .toList();
    }

    private List<LeaveApplication> matching(LocalDate start, LocalDate end) {
        return leaves.findAll().stream()
                .filter(item -> !item.getEndDate().isBefore(start) && !item.getStartDate().isAfter(end))
                .toList();
    }

    private long count(List<LeaveApplication> requests, LeaveStatus status) {
        return requests.stream().filter(item -> item.getStatus() == status).count();
    }

    private DateRange range(LocalDate requestedStart, LocalDate requestedEnd) {
        var today = LocalDate.now();
        var end = requestedEnd == null
                ? leaves.findAll().stream().map(LeaveApplication::getEndDate).max(Comparator.naturalOrder()).orElse(today)
                : requestedEnd;
        var start = requestedStart == null ? today.minusMonths(12) : requestedStart;
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("endDate must not precede startDate");
        }
        return new DateRange(start, end);
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
