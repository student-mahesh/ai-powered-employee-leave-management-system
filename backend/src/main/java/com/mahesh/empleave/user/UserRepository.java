package com.mahesh.empleave.user;

import com.mahesh.empleave.department.Department;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    List<AppUser> findByManager(AppUser manager);
    Page<AppUser> findByRole(Role role, Pageable pageable);
    boolean existsByDepartment(Department department);
}
