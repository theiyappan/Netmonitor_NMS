package com.network.snmp.repository.cassandra;

import com.network.snmp.model.InterfaceMetricDaily;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InterfaceMetricDailyRepository extends CassandraRepository<InterfaceMetricDaily, String> {

    @Query("SELECT * FROM interface_metrics_daily WHERE system_name = ?0 AND day_bucket >= ?1 AND day_bucket <= ?2 ALLOW FILTERING")
    List<InterfaceMetricDaily> findBySystemAndTime(String systemName, Instant start, Instant end);
}