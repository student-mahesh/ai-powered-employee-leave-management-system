package com.mahesh.empleave.employee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mahesh.empleave.department.Department;
import com.mahesh.empleave.user.AppUser;
import com.mahesh.empleave.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmployeeProfileTest {
    @Test
    void mapsAssignedDepartmentIdAndName() {
        var department = new Department("Computer", "COMP");
        ReflectionTestUtils.setField(department, "id", 1L);
        var user = new AppUser("employee@example.com", "hash", Role.EMPLOYEE, "Employee");
        ReflectionTestUtils.setField(user, "department", department);

        var profile = EmployeeProfile.from(user);

        assertEquals(1L, profile.departmentId());
        assertEquals("Computer", profile.departmentName());
    }

    @Test
    void mapsUnassignedDepartmentAsNull() {
        var user = new AppUser("manager@example.com", "hash", Role.MANAGER, "Manager");

        assertNull(EmployeeProfile.from(user).departmentName());
    }
}
