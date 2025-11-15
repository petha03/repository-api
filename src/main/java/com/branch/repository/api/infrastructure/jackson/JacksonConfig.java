package com.branch.repository.api.infrastructure.jackson;

import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * Jackson configuration for custom JSON serialization.
 * Uses Jackson2ObjectMapperBuilderCustomizer to register custom serializers.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer zonedDateTimeCustomizer() {
        return builder -> builder.serializers(
            new ZonedDateTimeSerializer(DateTimeFormatter.RFC_1123_DATE_TIME)
        );
    }
}