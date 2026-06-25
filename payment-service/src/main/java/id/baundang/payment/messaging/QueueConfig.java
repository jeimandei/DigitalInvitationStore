package id.baundang.payment.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
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

    // Declared here so the queue exists even when order-service is restarting.
    // Messages published during order-service downtime are held by RabbitMQ
    // and delivered once order-service reconnects.
    @Bean
    Queue orderPaymentProcessingQueue() {
        return QueueBuilder.durable("order.payment.processing").build();
    }

    @Bean
    Binding orderPaymentProcessingBinding(Queue orderPaymentProcessingQueue,
                                          TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderPaymentProcessingQueue)
                .to(ordersExchange)
                .with("order.paid");
    }
}
