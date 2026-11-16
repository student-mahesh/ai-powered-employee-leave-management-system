package com.mahesh.empleave.employee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import com.mahesh.empleave.audit.AuditService;
import com.mahesh.empleave.department.DepartmentRepository;
import com.mahesh.empleave.user.AppUser;
import com.mahesh.empleave.user.Role;
import com.mahesh.empleave.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class EmployeeProfileApiTest {
    @Test
    void meReturnsAssignedDepartmentNameForEmployee() {
        var users = mock(UserRepository.class);
        var employee = new AppUser("maheshsavkar93@gmail.com", "hash", Role.EMPLOYEE, "Mahesh Savkar");
        var department = new com.mahesh.empleave.department.Department("Computer", "COMP");
        ReflectionTestUtils.setField(department, "id", 1L);
        ReflectionTestUtils.setField(employee, "department", department);
        when(users.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
        var controller = new EmployeeController(users, mock(DepartmentRepository.class), mock(PasswordEncoder.class), mock(AuditService.class));

        var result = controller.me(new UsernamePasswordAuthenticationToken(employee.getEmail(), null));

        assertEquals("Computer", result.departmentName());
        assertEquals(1L, result.departmentId());
    }

    @Test
    void meReturnsNullDepartmentNameWhenEmployeeIsUnassigned() {
        var users = mock(UserRepository.class);
        var employee = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        when(users.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
        var controller = new EmployeeController(users, mock(DepartmentRepository.class), mock(PasswordEncoder.class), mock(AuditService.class));

        assertNull(controller.me(new UsernamePasswordAuthenticationToken(employee.getEmail(), null)).departmentName());
    }
}
