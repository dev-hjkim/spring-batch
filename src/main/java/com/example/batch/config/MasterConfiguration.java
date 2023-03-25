//package com.example.batch.config;
//
//import com.example.batch.domain.Transaction3;
//import org.springframework.amqp.core.AmqpTemplate;
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
//import org.springframework.batch.core.configuration.annotation.StepScope;
//import org.springframework.batch.core.step.tasklet.TaskletStep;
//import org.springframework.batch.integration.chunk.RemoteChunkingMasterStepBuilderFactory;
//import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
//import org.springframework.batch.item.file.FlatFileItemReader;
//import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//import org.springframework.core.io.Resource;
//import org.springframework.integration.amqp.dsl.Amqp;
//import org.springframework.integration.channel.DirectChannel;
//import org.springframework.integration.channel.QueueChannel;
//import org.springframework.integration.dsl.IntegrationFlow;
//import org.springframework.integration.dsl.IntegrationFlows;
//
//@Configuration
//@Profile("master")
//@EnableBatchIntegration
//public class MasterConfiguration {
//
//    @Autowired
//    private JobBuilderFactory jobBuilderFactory;
//
//    @Autowired
//    private RemoteChunkingMasterStepBuilderFactory remoteChunkingMasterStepBuilderFactory;
//
//    @Bean
//    public DirectChannel requests() {
//        return new DirectChannel();
//    }
//
//    @Bean
//    public IntegrationFlow outboundFlow(AmqpTemplate amqpTemplate) {
//        return IntegrationFlows.from(requests())
//                .handle(Amqp.outboundAdapter(amqpTemplate)
//                .routingKey("requests"))
//                .get();
//    }
//
//    @Bean
//    public QueueChannel replies() {
//        return new QueueChannel();
//    }
//
//    @Bean
//    public IntegrationFlow inboundFlow(ConnectionFactory connectionFactory) {
//        return IntegrationFlows.from(Amqp.inboundAdapter(connectionFactory, "replies"))
//                .channel(replies())
//                .get();
//    }
//
//    @Bean
//    @StepScope
//    public FlatFileItemReader<Transaction3> fileTransactionReader(
//            @Value("#{stepExecutionContext['file']}") Resource resource
//    ) {
////        Resource resource = new ClassPathResource("input/bigtransactions.csv");
//
//        return new FlatFileItemReaderBuilder<Transaction3>()
//                .name("transactionItemReader")
//                .resource(resource)
//                .saveState(false)
//                .delimited()
//                .names(new String[] {"account", "amount", "timestamp"})
//                .fieldSetMapper(fieldSet -> {
//                    Transaction3 transaction = new Transaction3();
//
//                    transaction.setAccount(fieldSet.readString("account"));
//                    transaction.setAmount(fieldSet.readBigDecimal("amount"));
//                    transaction.setTimestamp(fieldSet.readDate("timestamp", "yyyy-MM-dd HH:mm:ss"));
//
//                    return transaction;
//                })
//                .build();
//    }
//
//    @Bean
//    public TaskletStep masterStep() {
//        return this.remoteChunkingMasterStepBuilderFactory.get("masterStep")
//                .<Transaction3, Transaction3>chunk(100)
//                .reader(fileTransactionReader(null))
//                .outputChannel(requests())
//                .inputChannel(replies())
//                .build();
//    }
//
//    @Bean
//    public Job remoteChunkingJob() {
//        return this.jobBuilderFactory.get("remoteChunkingJob")
//                .start(masterStep())
//                .build();
//    }
//}
