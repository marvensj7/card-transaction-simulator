package com.marvens.capstone.service;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UtcClockConfiguration {
    @Bean
    public Clock transactionClock() { return Clock.systemUTC(); }
}
