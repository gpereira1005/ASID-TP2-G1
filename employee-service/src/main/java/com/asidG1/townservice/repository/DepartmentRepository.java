package com.asidG1.townservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.asidG1.townservice.model.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {


}
