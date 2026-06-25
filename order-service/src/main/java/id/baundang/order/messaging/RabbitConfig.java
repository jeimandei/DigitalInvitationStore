package id.baundang.order.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Bean
    TopicExchange ordersExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    Queue orderPaymentPaidQueue() {
        return QueueBuilder.durable("order.payment.processing").build();
    }

    @Bean
    Binding orderPaymentPaidBinding(Queue orderPaymentPaidQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderPaymentPaidQueue).to(ordersExchange).with("order.paid");
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
