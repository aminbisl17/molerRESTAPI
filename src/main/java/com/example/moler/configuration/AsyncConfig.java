package com.example.moler.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Enables @Async so push notifications are sent without blocking requests
@Configuration
@EnableAsync
public class AsyncConfig {
}
