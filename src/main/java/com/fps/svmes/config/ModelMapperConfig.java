package com.fps.svmes.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


import com.fasterxml.jackson.databind.ObjectMapper;

import com.fps.svmes.utils.UserIdToCommonUserDTOConverter;
import org.modelmapper.ModelMapper;

import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    private final UserIdToCommonUserDTOConverter userConverter;

    public ModelMapperConfig(UserIdToCommonUserDTOConverter userConverter) {
        this.userConverter = userConverter;
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
//        mapper.getConfiguration().setSkipNullEnabled(true);

        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setSkipNullEnabled(true);

        registerGlobalConverters(mapper);

        return mapper;
    }

    private void registerGlobalConverters(ModelMapper modelMapper) {
        modelMapper.addConverter(userConverter);
    }

    @Bean
    public ObjectMapper objectMapper() {
//        return new ObjectMapper(); // Use for JSON serialization/deserialization
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // Support for Java 8 Date/Time types
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Use ISO-8601 format
        return mapper;
    }
}
