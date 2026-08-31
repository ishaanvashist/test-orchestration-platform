package dev.ishaan.test_orchestration_platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class CacheEvictionService {

    private static final Logger logger = LoggerFactory.getLogger(CacheEvictionService.class);

    @CacheEvict(value = "testHistory", key = "#testName")
    public void evictTestHistoryCache(String testName) {
        logger.info("Evicting stale cache for: {}", testName);
    }
}