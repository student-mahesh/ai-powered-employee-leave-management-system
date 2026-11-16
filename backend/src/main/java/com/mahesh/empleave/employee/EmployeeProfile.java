package com.mahesh.empleave.employee;

import com.mahesh.empleave.user.AppUser;
import com.mahesh.empleave.user.Role;

record EmployeeProfile(Long id, String email, String fullName, Role role, Long departmentId, String departmentName) {
    static EmployeeProfile from(AppUser user) {
        var department = user.getDepartment();
        return new EmployeeProfile(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), department == null ? null : department.getId(), department == null ? null : department.getName());
    }
}
