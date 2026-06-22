package id.baundang.payment.messaging;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {

    @Value("${app.rabbitmq.orders-exchange}")
    private String ordersExchange;

    @Bean
    Queue orderCreatedQueue() {
        return QueueBuilder.durable("payment.order.created").build();
    }

    @Bean
    Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(ordersExchange)
                .with("order.created");
    }
}
