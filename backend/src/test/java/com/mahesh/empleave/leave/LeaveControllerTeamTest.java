package com.mahesh.empleave.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahesh.empleave.user.AppUser;
import com.mahesh.empleave.user.Role;
import com.mahesh.empleave.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class LeaveControllerTeamTest {
    @Test
    void managerTeamEndpointUsesDirectReportQueryRatherThanPendingOnly() {
        var service = Mockito.mock(LeaveService.class);
        var repository = Mockito.mock(LeaveRepository.class);
        var users = Mockito.mock(UserRepository.class);
        var controller = new LeaveController(service, repository, users);
        var manager = new AppUser("manager@example.com", "hash", Role.MANAGER, "Manager");
        var authentication = new UsernamePasswordAuthenticationToken("manager@example.com", null);
        when(users.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));
        when(repository.findByApplicantManagerOrderByIdDesc(any(), any())).thenReturn(new PageImpl<>(List.of()));

        var result = controller.team(authentication, 0, 25);

        assertEquals(0, result.getTotalElements());
        verify(repository).findByApplicantManagerOrderByIdDesc(any(), any());
    }

    @Test
    void leaveDtoMapsApplicantFullNameForTeamResponses() {
        var employee = new AppUser("mahesh@example.com", "hash", Role.EMPLOYEE, "Mahesh Savkar");
        var leave = new LeaveApplication(employee, "Annual Leave", LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 25), 6, "Personal");

        var result = com.mahesh.empleave.common.dto.ApiDtos.leave(leave);

        assertEquals("Mahesh Savkar", result.applicantName());
        assertEquals(leave.getApplicant().getId(), result.applicantId());
    }
}
