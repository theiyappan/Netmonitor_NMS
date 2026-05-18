package com.network.snmp.repository.mysql;

import com.network.snmp.entity.mysql.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    List<SystemLog> findBySystemNameAndLevelAndTimestampAfterOrderByTimestampDesc(String systemName, String level, Instant timestamp);

    List<SystemLog> findBySystemNameAndLevelNotAndTimestampAfterOrderByTimestampDesc(String systemName, String level, Instant timestamp);

    List<SystemLog> findBySystemNameAndTimestampAfterOrderByTimestampDesc(String systemName, Instant timestamp);

    void deleteByTimestampBefore(Instant timestamp);
}