package com.example.batch.config;

import com.example.batch.domain.Customer2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.jms.JmsItemReader;
import org.springframework.batch.item.jms.JmsItemWriter;
import org.springframework.batch.item.jms.builder.JmsItemReaderBuilder;
import org.springframework.batch.item.jms.builder.JmsItemWriterBuilder;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.oxm.xstream.XStreamMarshaller;

import javax.jms.ConnectionFactory;
import java.util.HashMap;
import java.util.Map;


@EnableBatchProcessing
@Configuration
public class FormattedTextFileJob {
    private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;

    public FormattedTextFileJob(JobBuilderFactory jobBuilderFactory,
                                StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(connectionFactory);
        cachingConnectionFactory.afterPropertiesSet();

        JmsTemplate jmsTemplate = new JmsTemplate(cachingConnectionFactory);
        jmsTemplate.setDefaultDestinationName("customers");
        jmsTemplate.setMessageConverter(jacksonJmsMessageConverter());
        jmsTemplate.setReceiveTimeout(500L);

        return jmsTemplate;
    }

    //////////////////////////// STEP 1 ////////////////////////////

    @Bean
    public FlatFileItemReader<Customer2> customerFileReader() {
        Resource inputFile = new ClassPathResource("input/customer.csv");

        return new FlatFileItemReaderBuilder<Customer2>()
                .name("customerFileReader")
                .delimited()
                .names(new String[] {"firstName",
                        "middleInitial",
                        "lastName",
                        "address",
                        "city",
                        "state",
                        "zip"})
                .targetType(Customer2.class)
                .resource(inputFile)
                .build();
    }

    @Bean
    public StaxEventItemWriter<Customer2> xmlOutputWriter() {
        Resource outputFile = new FileSystemResource("output/jmsCustomer.xml");

        Map<String, Class> aliases = new HashMap<>();
        aliases.put("customer", Customer2.class);

        XStreamMarshaller marshaller = new XStreamMarshaller();
        marshaller.setAliases(aliases);

        return new StaxEventItemWriterBuilder<Customer2>()
                .name("xmlOutputWriter")
                .resource(outputFile)
                .marshaller(marshaller)
                .rootTagName("customers")
                .build();
    }

    @Bean
    public JmsItemReader<Customer2> jmsItemReader(JmsTemplate jmsTemplate) {
        return new JmsItemReaderBuilder<Customer2>()
                .jmsTemplate(jmsTemplate)
                .itemType(Customer2.class)
                .build();
    }

    @Bean
    public JmsItemWriter<Customer2> jmsItemWriter(JmsTemplate jmsTemplate) {
        return new JmsItemWriterBuilder<Customer2>()
                .jmsTemplate(jmsTemplate)
                .build();
    }

    @Bean
    public Step formatInputStep() throws Exception {
        return this.stepBuilderFactory.get("formatInputStep")
                .<Customer2, Customer2>chunk(10)
                .reader(customerFileReader())
                .writer(jmsItemWriter(null))
                .build();
    }

    @Bean
    public Step formatOutputStep() throws Exception {
        return this.stepBuilderFactory.get("formatOutputStep")
                .<Customer2, Customer2>chunk(10)
                .reader(jmsItemReader(null))
                .writer(xmlOutputWriter())
                .build();
    }

    @Bean
    public Job jmsFormatJob() throws Exception {
        return this.jobBuilderFactory.get("jmsFormatJob")
                .start(formatInputStep())
                .next(formatOutputStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
