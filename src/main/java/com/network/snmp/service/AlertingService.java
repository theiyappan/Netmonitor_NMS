package com.network.snmp.service;

import com.network.snmp.model.Alert;
import com.network.snmp.repository.mysql.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlertingService {

    private final AlertRepository alertRepository;
    private final JavaMailSender mailSender;

    @Value("${alerting.recipient-email}")
    private String recipientEmail;

    public void triggerAlert(String systemName, Integer ifIndex, String component, String alertType, String message) {
        System.out.println(">>> [AlertingService] Processing trigger: " + alertType);

        Optional<Alert> existing = alertRepository.findBySystemNameAndInterfaceIndexAndAlertTypeAndActive(
                systemName, ifIndex, alertType, true);

        if (existing.isPresent()) {
            if (!"TEST".equalsIgnoreCase(component)) {
                System.out.println(">>> [AlertingService] Alert already active. Skipping.");
                return;
            }
        }
        Alert alert = existing.orElse(Alert.builder()
                .systemName(systemName)
                .interfaceIndex(ifIndex)
                .component(component)
                .alertType(alertType)
                .severity("CRITICAL")
                .active(true)
                .startTime(Instant.now())
                .build());

        alert.setMessage(message);
        alert.setLastActiveTime(Instant.now());
        alertRepository.save(alert);

        sendEmail(systemName, alertType, message);
    }

    private void sendEmail(String systemName, String subject, String body) {
        if (recipientEmail == null || recipientEmail.isEmpty()) {
            System.err.println(">>> [Email] No recipient configured in application.yml");
            return;
        }

        try {
            System.out.println(">>> [Email] Attempting to send to: " + recipientEmail);

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom("snmpmonitor1@gmail.com");
            mail.setTo(recipientEmail);
            mail.setSubject("ALERT: " + systemName + " - " + subject);
            mail.setText(body);

            mailSender.send(mail);

            System.out.println(">>> [Email] ✅ SUCCESS! Email sent.");
        } catch (Exception e) {
            System.err.println(">>> [Email] ❌ FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void resolveAlert(String systemName, Integer ifIndex, String alertType) {
        Optional<Alert> existing = alertRepository.findBySystemNameAndInterfaceIndexAndAlertTypeAndActive(
                systemName, ifIndex, alertType, true);

        existing.ifPresent(alert -> {
            alert.setActive(false);
            alert.setEndTime(Instant.now());
            alertRepository.save(alert);
            System.out.println(">>> [AlertingService] Alert Resolved: " + alertType);
        });
    }
}