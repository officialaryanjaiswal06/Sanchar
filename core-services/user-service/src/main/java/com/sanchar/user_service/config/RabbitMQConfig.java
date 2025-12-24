package com.sanchar.user_service.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "user.exchange";
    public static final String QUEUE = "user.registered.queue";
    public static final String ROUTING_KEY = "user.registered.key";

    @Bean
    public Queue registerQueue() { return new Queue(QUEUE, true); }

    @Bean
    public TopicExchange userExchange() { return new TopicExchange(EXCHANGE); }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }
}
