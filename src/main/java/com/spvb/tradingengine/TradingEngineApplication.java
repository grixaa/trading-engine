package com.spvb.tradingengine;

import lombok.NonNull;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;

@SpringBootApplication
public class TradingEngineApplication {
    public static void main(@NonNull final String[] args) {
        SpringApplication.run(TradingEngineApplication.class, args);
    }
}
