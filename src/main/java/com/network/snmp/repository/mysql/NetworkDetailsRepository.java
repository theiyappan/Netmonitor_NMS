package com.network.snmp.repository.mysql;

import com.network.snmp.model.NetworkDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NetworkDetailsRepository extends JpaRepository<NetworkDetails, String> {
    Optional<NetworkDetails> findBySystemName(String systemName);
}