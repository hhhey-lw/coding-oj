package com.longoj.top.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class JudgeMQConfig {

    public static final String QUEUE_NAME = "judgeQueue";
    public static final String EXCHANGE_NAME = "judgeExchange";
    public static final String ROUTING_KEY = "judgeRoutingKey";

    @Bean
    public Queue judgeQueue() {
        // 创建一个名为 "judgeQueue" 的队列
        return new Queue(QUEUE_NAME, true, false, false);
    }

    @Bean
    public Exchange judgeExchange() {
        // 创建一个名为 "judgeExchange" 的交换机
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Binding judgeBinding(Queue judgeQueue, Exchange judgeExchange) {
        // 将队列和交换机绑定
        return BindingBuilder.bind(judgeQueue).to(judgeExchange).with(ROUTING_KEY).noargs();
    }

}
