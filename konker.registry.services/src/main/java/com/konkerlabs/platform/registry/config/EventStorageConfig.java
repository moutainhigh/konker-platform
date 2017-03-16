package com.konkerlabs.platform.registry.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Data;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
public class EventStorageConfig {

    private String eventRepositoryBean;
    public EventStorageConfig() {
        Map<String, Object> defaultMap = new HashMap<>();
        defaultMap.put("eventstorage.repositorybean", "cassandraEvents");
        Config defaultConf = ConfigFactory.parseMap(defaultMap);
        Config config = ConfigFactory.load().withFallback(defaultConf);
        setEventRepositoryBean(config.getString("eventstorage.repositorybean"));
    }
}
