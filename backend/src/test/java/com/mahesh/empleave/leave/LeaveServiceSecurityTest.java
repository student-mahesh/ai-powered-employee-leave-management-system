package com.mahesh.empleave.leave;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.mahesh.empleave.audit.AuditService;
import com.mahesh.empleave.holiday.HolidayRepository;
import com.mahesh.empleave.leavebalance.BalanceTransactionRepository;
import com.mahesh.empleave.leavebalance.LeaveBalanceRepository;
import com.mahesh.empleave.leavetype.LeaveTypeRepository;
import com.mahesh.empleave.notification.NotificationService;
import com.mahesh.empleave.user.AppUser;
import com.mahesh.empleave.user.Role;
import com.mahesh.empleave.user.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LeaveServiceSecurityTest {
    @Mock LeaveRepository leaves;
    @Mock UserRepository users;
    @Mock LeaveTypeRepository types;
    @Mock LeaveBalanceRepository balances;
    @Mock BalanceTransactionRepository transactions;
    @Mock HolidayRepository holidays;
    @Mock AuditService audit;
    @Mock NotificationService notifications;
    @InjectMocks LeaveService service;

    @Test
    void applicantCannotRejectTheirOwnLeaveEvenIfTheyAreAssignedAsManager() {
        var applicant = user(7L, "manager@example.com", Role.MANAGER);
        ReflectionTestUtils.setField(applicant, "manager", applicant);
        var leave = new LeaveApplication(applicant, "Annual", LocalDate.now().plusDays(3), LocalDate.now().plusDays(3), 1, "Personal");
        ReflectionTestUtils.setField(leave, "id", 12L);
        when(users.findByEmail(applicant.getEmail())).thenReturn(Optional.of(applicant));
        when(leaves.findById(12L)).thenReturn(Optional.of(leave));

        assertThrows(IllegalArgumentException.class, () -> service.reject(applicant.getEmail(), 12L, "No"));
        verifyNoInteractions(audit, notifications);
    }

    @Test
    void managerCannotRejectLeaveOutsideDirectReportScope() {
        var manager = user(8L, "manager@example.com", Role.MANAGER);
        var employee = user(9L, "employee@example.com", Role.EMPLOYEE);
        var leave = new LeaveApplication(employee, "Annual", LocalDate.now().plusDays(3), LocalDate.now().plusDays(3), 1, "Personal");
        ReflectionTestUtils.setField(leave, "id", 13L);
        when(users.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        when(leaves.findById(13L)).thenReturn(Optional.of(leave));

        assertThrows(SecurityException.class, () -> service.reject(manager.getEmail(), 13L, "No"));
        verifyNoInteractions(audit, notifications);
    }

    private AppUser user(Long id, String email, Role role) {
        var user = new AppUser(email, "hash", role, email);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
