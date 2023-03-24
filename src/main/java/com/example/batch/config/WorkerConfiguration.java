package com.example.batch.config;

import com.example.batch.domain.Transaction3;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.batch.integration.chunk.RemoteChunkingWorkerBuilder;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;

import javax.sql.DataSource;

@Configuration
@Profile("!master")
@EnableBatchIntegration
public class WorkerConfiguration {

    @Autowired
    private RemoteChunkingWorkerBuilder<Transaction3, Transaction3> workerBuilder;

    @Bean
    public DirectChannel requests() {
        return new DirectChannel();
    }

    @Bean
    public DirectChannel replies() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow inboundFlow(ConnectionFactory connectionFactory) {
        return IntegrationFlows.from(Amqp.inboundAdapter(connectionFactory, "requests"))
                .channel(requests())
                .get();
    }

    @Bean
    public IntegrationFlow outboundFlow(AmqpTemplate template) {
        return IntegrationFlows.from(replies())
                .handle(Amqp.outboundAdapter(template)
                .routingKey("replies"))
                .get();
    }

    @Bean
    public IntegrationFlow integrationFlow() {
        return this.workerBuilder
                .itemProcessor(processor())
                .itemWriter(writer(null))
                .inputChannel(requests())
                .outputChannel(replies())
                .build();
    }

    @Bean
    public ItemProcessor<Transaction3, Transaction3> processor() {
        return transaction -> {
            System.out.println("processing transaction = " + transaction);
            return transaction;
        };
    }

    @Bean
    public JdbcBatchItemWriter<Transaction3> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaction3>()
                .dataSource(dataSource)
                .sql("INSERT INTO TRANSACTION3 (ACCOUNT, AMOUNT, TIMESTAMP) " +
                        "VALUES (:account, :amount, :timestamp)")
                .beanMapped()
                .build();
    }
}
