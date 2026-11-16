package com.mahesh.empleave.leavetype;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mahesh.empleave.audit.AuditService;
import com.mahesh.empleave.user.AppUser;
import com.mahesh.empleave.user.Role;
import com.mahesh.empleave.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

class LeaveTypeControllerTest {
  @Test
  void activePersistsTheRequestedStatusAndReturnsIt() {
    var repository = mock(LeaveTypeRepository.class);
    var users = mock(UserRepository.class);
    var audit = mock(AuditService.class);
    var controller = new LeaveTypeController(repository, users, audit);
    var type = new LeaveType("Annual Leave");
    ReflectionTestUtils.setField(type, "id", 7L);
    var admin = user(1L, "admin@example.test", Role.ADMIN, "Admin");
    when(repository.findById(7L)).thenReturn(Optional.of(type));
    when(repository.save(type)).thenReturn(type);
    when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

    var result = controller.active(new UsernamePasswordAuthenticationToken(admin.getEmail(), null), 7L, false);

    assertFalse(result.active());
    verify(repository).save(type);
    verify(audit).log(admin, "LEAVE_TYPE_UPDATED", "LEAVE_TYPE", 7L);
  }

  private AppUser user(Long id, String email, Role role, String name) {
    var user = new AppUser(email, "hash", role, name);
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }
}
