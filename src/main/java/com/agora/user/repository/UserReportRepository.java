package com.agora.user.repository;

import com.agora.user.model.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    boolean existsByReporterIdAndReportedId(Long reporterId, Long reportedId);

    long countByReportedId(Long reportedId);

    @Modifying
    void deleteByReportedId(Long reportedId);
}
