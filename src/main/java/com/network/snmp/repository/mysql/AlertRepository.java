package com.network.snmp.repository.mysql;

import com.network.snmp.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Finds an active alert so we don't spam emails (unless it's a TEST)
    Optional<Alert> findBySystemNameAndInterfaceIndexAndAlertTypeAndActive(
            String systemName,
            Integer interfaceIndex,
            String alertType,
            boolean active
    );

    List<Alert> findBySystemNameAndActive(String systemName, boolean active);
}