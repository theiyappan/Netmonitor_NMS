package com.network.snmp.repository.cassandra;

import com.network.snmp.model.InterfaceMetricHourly;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InterfaceMetricHourlyRepository extends CassandraRepository<InterfaceMetricHourly, String> {
    @Query("SELECT * FROM interface_metrics_hourly WHERE system_name = ?0 AND hour_bucket >= ?1 AND hour_bucket <= ?2 ALLOW FILTERING")
    List<InterfaceMetricHourly> findBySystemAndTime(String systemName, Instant start, Instant end);
}