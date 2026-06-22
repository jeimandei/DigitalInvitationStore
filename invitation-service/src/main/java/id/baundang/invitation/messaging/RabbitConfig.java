package id.baundang.invitation.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.rabbitmq.orders-exchange}")
    private String ordersExchange;

    @Value("${app.rabbitmq.rsvp-exchange}")
    private String rsvpExchange;

    @Value("${app.rabbitmq.invitations-exchange}")
    private String invitationsExchange;

    @Bean
    TopicExchange ordersTopicExchange() {
        return new TopicExchange(ordersExchange, true, false);
    }

    @Bean
    TopicExchange rsvpTopicExchange() {
        return new TopicExchange(rsvpExchange, true, false);
    }

    @Bean
    TopicExchange invitationsTopicExchange() {
        return new TopicExchange(invitationsExchange, true, false);
    }

    @Bean
    Queue invitationOrderPaidQueue() {
        return QueueBuilder.durable("invitation.order.paid").build();
    }

    @Bean
    Binding invitationOrderPaidBinding(Queue invitationOrderPaidQueue,
                                       TopicExchange ordersTopicExchange) {
        return BindingBuilder.bind(invitationOrderPaidQueue)
                .to(ordersTopicExchange)
                .with("order.paid");
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
