package id.baundang.notification.messaging;

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

    @Bean
    Queue notifOrderCreatedQueue() {
        return QueueBuilder.durable("notification.order.created").build();
    }

    @Bean
    Queue notifOrderPaidQueue() {
        return QueueBuilder.durable("notification.order.paid").build();
    }

    @Bean
    Queue notifOrderRevisedQueue() {
        return QueueBuilder.durable("notification.order.revised").build();
    }

    @Bean
    Queue notifRsvpSubmittedQueue() {
        return QueueBuilder.durable("notification.rsvp.submitted").build();
    }

    @Bean
    Queue notifInvitationExpiringQueue() {
        return QueueBuilder.durable("notification.invitation.expiring").build();
    }

    @Bean
    Queue notifGiftConfirmedQueue() {
        return QueueBuilder.durable("notification.gift.confirmed").build();
    }

    @Bean
    Queue notifRevisionCompletedQueue() {
        return QueueBuilder.durable("notification.revision.completed").build();
    }

    // --- bindings ---

    @Bean
    Binding orderCreatedBinding(Queue notifOrderCreatedQueue, TopicExchange ordersTopicExchange) {
        return BindingBuilder.bind(notifOrderCreatedQueue).to(ordersTopicExchange).with("order.created");
    }

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
    Binding invitationExpiringBinding(Queue notifInvitationExpiringQueue,
                                      TopicExchange invitationsTopicExchange) {
        return BindingBuilder.bind(notifInvitationExpiringQueue)
                .to(invitationsTopicExchange).with("invitation.expiring");
    }

    @Bean
    Binding giftConfirmedBinding(Queue notifGiftConfirmedQueue, TopicExchange rsvpTopicExchange) {
        return BindingBuilder.bind(notifGiftConfirmedQueue).to(rsvpTopicExchange).with("gift.confirmed");
    }

    @Bean
    Binding revisionCompletedBinding(Queue notifRevisionCompletedQueue,
                                     TopicExchange ordersTopicExchange) {
        return BindingBuilder.bind(notifRevisionCompletedQueue)
                .to(ordersTopicExchange).with("revision.completed");
    }
}
