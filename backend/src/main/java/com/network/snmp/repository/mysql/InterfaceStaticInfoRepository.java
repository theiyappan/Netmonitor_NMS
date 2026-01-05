package com.network.snmp.repository.mysql;

import com.network.snmp.entity.mysql.InterfaceStaticInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterfaceStaticInfoRepository extends JpaRepository<InterfaceStaticInfo, Long> {

    List<InterfaceStaticInfo> findAllBySystemName(String systemName);

    Optional<InterfaceStaticInfo> findBySystemNameAndInterfaceIndex(String systemName, Integer interfaceIndex);
}