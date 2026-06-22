package id.baundang.notification.messaging;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {

    @Value("${app.rabbitmq.orders-exchange}")
    private String ordersExchange;

    @Value("${app.rabbitmq.rsvp-exchange}")
    private String rsvpExchange;

    @Value("${app.rabbitmq.invitations-exchange}")
    private String invitationsExchange;

    // --- exchanges ---

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

    // --- queues ---

    @Bean Queue notifOrderPaidQueue()        { return QueueBuilder.durable("notification.order.paid").build(); }
    @Bean Queue notifOrderRevisedQueue()     { return QueueBuilder.durable("notification.order.revised").build(); }
    @Bean Queue notifRsvpSubmittedQueue()    { return QueueBuilder.durable("notification.rsvp.submitted").build(); }
    @Bean Queue notifInvitationExpiringQueue() { return QueueBuilder.durable("notification.invitation.expiring").build(); }

    // --- bindings ---

    @Bean
    Binding orderPaidBinding(Queue notifOrderPaidQueue, TopicExchange ordersTopicExchange) {
        return BindingBuilder.bind(notifOrderPaidQueue).to(ordersTopicExchange).with("order.paid");
    }

    @Bean
    Binding orderRevisedBinding(Queue notifOrderRevisedQueue, TopicExchange ordersTopicExchange) {
        return BindingBuilder.bind(notifOrderRevisedQueue).to(ordersTopicExchange).with("order.revised");
    }

    @Bean
    Binding rsvpSubmittedBinding(Queue notifRsvpSubmittedQueue, TopicExchange rsvpTopicExchange) {
        return BindingBuilder.bind(notifRsvpSubmittedQueue).to(rsvpTopicExchange).with("rsvp.submitted");
    }

    @Bean
    Binding invitationExpiringBinding(Queue notifInvitationExpiringQueue, TopicExchange invitationsTopicExchange) {
        return BindingBuilder.bind(notifInvitationExpiringQueue).to(invitationsTopicExchange).with("invitation.expiring");
    }
}
