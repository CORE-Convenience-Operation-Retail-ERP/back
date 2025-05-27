package com.core.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .defaultContentType(MediaType.APPLICATION_JSON)
                .favorParameter(true)
                .parameterName("mediaType")
                .ignoreAcceptHeader(false)
                .useRegisteredExtensionsOnly(false)
                .mediaType("xml", MediaType.APPLICATION_XML)
                .mediaType("json", MediaType.APPLICATION_JSON);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        converters.add(new MappingJackson2HttpMessageConverter());
        converters.add(new Jaxb2RootElementHttpMessageConverter());
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // XML 및 JSON 변환을 위한 컨버터 추가
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        
        // StringHttpMessageConverter 추가 (UTF-8)
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setSupportedMediaTypes(
                Arrays.asList(MediaType.TEXT_PLAIN, MediaType.TEXT_XML, new MediaType("text", "xml", StandardCharsets.UTF_8))
        );
        messageConverters.add(stringConverter);
        
        // XML 변환을 위한 컨버터
        Jaxb2RootElementHttpMessageConverter xmlConverter = new Jaxb2RootElementHttpMessageConverter();
        xmlConverter.setSupportedMediaTypes(
                Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML, new MediaType("text", "xml", StandardCharsets.UTF_8))
        );
        messageConverters.add(xmlConverter);
        
        // JSON 변환을 위한 컨버터
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(
                Collections.singletonList(MediaType.APPLICATION_JSON)
        );
        messageConverters.add(jsonConverter);
        
        restTemplate.setMessageConverters(messageConverters);
        
        return restTemplate;
    }
} 