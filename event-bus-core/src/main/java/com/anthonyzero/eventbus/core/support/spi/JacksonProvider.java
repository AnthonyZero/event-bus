package com.anthonyzero.eventbus.core.support.spi;

import com.anthonyzero.eventbus.core.exception.EventBusException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * jackson
 *
 */
public class JacksonProvider implements IJson {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    @Override
    public String className() {
        return "com.fasterxml.jackson.databind.ObjectMapper";
    }

    @Override
    public String toJsonString(Object value) {
        try {
            return JacksonUtil.MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new EventBusException(e);
        }
    }

    @Override
    public <T> T parseObject(String text, Type type) {
        try {
            return JacksonUtil.MAPPER.readValue(text, JacksonUtil.MAPPER.constructType(type));
        } catch (IOException e) {
            throw new EventBusException(e);
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }

    private static class JacksonUtil {
        private static final ObjectMapper MAPPER = new ObjectMapper();

        static {
            MAPPER.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
            MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            MAPPER.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
            MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            MAPPER.disable(SerializationFeature.INDENT_OUTPUT);

            JavaTimeModule javaTimeModule = new JavaTimeModule();
            javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));

            javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
            MAPPER.registerModule(javaTimeModule);
        }
    }
}
