package com.roadsaathi.backend.service;

import com.roadsaathi.backend.repository.HazardReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExpiryService {

    private final HazardReportRepository hazardReportRepository;

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void expireReports() {
        int expiredCount = hazardReportRepository.expireActiveReports();
        if (expiredCount > 0) {
            log.info("Expired {} unconfirmed hazard reports", expiredCount);
        }
    }
}
