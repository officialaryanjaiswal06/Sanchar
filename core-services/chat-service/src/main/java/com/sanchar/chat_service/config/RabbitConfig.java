package com.sanchar.chat_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter; // Generic Interface
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String CHAT_EXCHANGE = "sanchar.chat.exchange";
    public static final String DB_QUEUE = "sanchar.chat.db.write";
    public static final String ROUTING_KEY_PATTERN = "chat.room.#";

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE);
    }


    @Bean
    public Queue persistenceQueue() {
        return new Queue(DB_QUEUE, true);
    }

    @Bean
    public Binding binding(Queue persistenceQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(persistenceQueue)
                .to(chatExchange)
                .with(ROUTING_KEY_PATTERN);
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        return rabbitTemplate;
    }
}
