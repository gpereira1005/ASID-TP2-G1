package com.asidG1.studentservice.repository;

import com.asidG1.studentservice.model.entity.Mark;
import com.asidG1.studentservice.model.entity.enums.MarkEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarkRepository extends JpaRepository<Mark, Long> {

    Mark findByMark(MarkEnum mark);
}
