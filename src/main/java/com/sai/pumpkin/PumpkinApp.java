package com.sai.pumpkin;

import com.google.common.base.Predicates;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Created by saipkri on 07/09/16.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"com.sai.pumpkin"})
@EnableSwagger2
@EnableAsync
@EnableMongoRepositories
@EnableCaching
@EnableScheduling
public class PumpkinApp {

    @Value("${amqBrokerUrl:#{null}}")
    private String amqBrokerUrl;

    @Value("${redisServer}")
    private String redisServer;

    private static final Logger LOGGER = Logger.getLogger(PumpkinApp.class);

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    /*@Bean
    public JedisConnectionFactory redisConnectionFactory() {
        JedisConnectionFactory redisConnectionFactory = new JedisConnectionFactory();

        // Defaults
        redisConnectionFactory.setHostName(redisServer);
        redisConnectionFactory.setPort(6379);
        return redisConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>();
        redisTemplate.setConnectionFactory(cf);
        return redisTemplate;
    }

    @Bean
    public CacheManager cacheManager(RedisTemplate redisTemplate) {
        RedisCacheManager cacheManager = new RedisCacheManager(redisTemplate);

        // Number of seconds before expiration. Defaults to unlimited (0)
        cacheManager.setDefaultExpiration(3000);
        return cacheManager;
    }*/

    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    CacheManager cacheManager() {
        return new HazelcastCacheManager(hazelcastInstance());
    }


    /**
     * Swagger 2 docket bean configuration.
     *
     * @return swagger 2 Docket.
     */
    @Bean
    public Docket configApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("config")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(Predicates.not(PathSelectors.regex("/error"))) // Exclude Spring error controllers
                .build();
    }


    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Pumpkin APP REST API")
                .contact("sai@concordesearch.co.uk")
                .version("1.0")
                .build();
    }

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        System.out.println(" ------- ActiveMQConnectionFactory Factory ----------");
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(amqBrokerUrl);
        return connectionFactory;
    }

    @Bean
    public JmsTemplate jmsTemplate(final ActiveMQConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setDefaultDestinationName("artifact.collection.queue");
        return jmsTemplate;
    }

    public static void main(String[] args) {
        SpringApplicationBuilder application = new SpringApplicationBuilder();
        application //
                .headless(true) //
                .addCommandLineProperties(true) //
                .sources(PumpkinApp.class) //
                .main(PumpkinApp.class) //
                .registerShutdownHook(true)
                .run(args);
        LOGGER.info(" ------------- " + application + " --------------------- STARTED ----------- ");
    }


}
