package com.mahesh.empleave.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mahesh.empleave.leave.LeaveApplication;
import com.mahesh.empleave.leave.LeaveRepository;
import com.mahesh.empleave.leave.LeaveStatus;
import com.mahesh.empleave.user.AppUser;
import com.mahesh.empleave.user.Role;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ReportControllerTest {
    @Test
    void summaryAggregatesOnlyRequestsStartingWithinTheRequestedRange() {
        var repository = Mockito.mock(LeaveRepository.class);
        var controller = new ReportController(repository);
        var applicant = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var pending = new LeaveApplication(applicant, "Annual", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12), 1, "Annual leave");
        var approved = new LeaveApplication(applicant, "Sick", LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 21), 2, "Sick leave");
        approved.approve();
        var outsideRange = new LeaveApplication(applicant, "Annual", LocalDate.of(2025, 12, 30), LocalDate.of(2025, 12, 31), 2, "Earlier leave");
        when(repository.findAll()).thenReturn(List.of(pending, approved, outsideRange));

        var result = controller.summary(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertEquals(2, result.totalRequests());
        assertEquals(1, result.pending());
        assertEquals(1, result.approved());
        assertEquals(0, result.rejected());
        assertEquals(3, result.requestedWorkingDays());
        assertEquals(1L, result.byLeaveType().get("Annual"));
        assertEquals(1L, result.byLeaveType().get("Sick"));
        assertEquals(LeaveStatus.PENDING, pending.getStatus());
    }

    @Test
    void defaultRangeIncludesFutureApprovedLeaveThroughItsEndDate() {
        var repository = Mockito.mock(LeaveRepository.class);
        var controller = new ReportController(repository);
        var applicant = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var approved = new LeaveApplication(applicant, "Annual Leave", LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 25), 6, "Annual leave");
        approved.approve();
        when(repository.findAll()).thenReturn(List.of(approved));

        var result = controller.summary(null, null);

        assertEquals(1, result.totalRequests());
        assertEquals(1, result.approved());
        assertEquals(6, result.requestedWorkingDays());
        assertEquals(1L, result.byLeaveType().get("Annual Leave"));
        assertEquals(LocalDate.of(2026, 11, 25), result.endDate());
    }

    @Test
    void matchingIncludesRequestsThatOverlapTheDateRangeAtEitherBoundary() {
        var repository = Mockito.mock(LeaveRepository.class);
        var controller = new ReportController(repository);
        var applicant = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        var spanning = new LeaveApplication(applicant, "Annual Leave", LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 18), 7, "Annual leave");
        when(repository.findAll()).thenReturn(List.of(spanning));

        var result = controller.summary(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 25));

        assertEquals(1, result.totalRequests());
        assertEquals(7, result.requestedWorkingDays());
    }
}
