package com.network.snmp.repository.cassandra;

import com.network.snmp.model.InterfaceMetric;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InterfaceMetricRepository extends CassandraRepository<InterfaceMetric, String> {

    @Query("SELECT * FROM interface_metrics WHERE system_name = :systemName LIMIT 1000")
    List<InterfaceMetric> findBySystemName(@Param("systemName") String systemName);

    @Query("SELECT * FROM interface_metrics WHERE system_name = :systemName AND snap_time > :minTime ALLOW FILTERING")
    List<InterfaceMetric> findBySystemNameRecent(
            @Param("systemName") String systemName,
            @Param("minTime") Instant minTime
    );

    @Query("SELECT * FROM interface_metrics WHERE system_name = :systemName AND snap_time >= :startTime AND snap_time <= :endTime ALLOW FILTERING")
    List<InterfaceMetric> findBySystemNameAndTimeRange(
            @Param("systemName") String systemName,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query("SELECT * FROM interface_metrics WHERE system_name = :systemName AND interface_index = :interfaceIndex ALLOW FILTERING")
    List<InterfaceMetric> findBySystemNameAndInterfaceIndex(
            @Param("systemName") String systemName,
            @Param("interfaceIndex") Integer interfaceIndex
    );

    @Query("SELECT * FROM interface_metrics WHERE system_name = :systemName AND interface_index = :interfaceIndex AND snap_time >= :startTime AND snap_time <= :endTime ALLOW FILTERING")
    List<InterfaceMetric> findBySystemNameAndInterfaceIndexAndTimeRange(
            @Param("systemName") String systemName,
            @Param("interfaceIndex") Integer interfaceIndex,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
}