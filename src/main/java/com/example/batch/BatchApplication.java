package com.example.batch;

import com.example.batch.domain.Customer2;
import com.example.batch.service.UpperCaseNameService;
import com.example.batch.validator.UniqueLastNameValidator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.adapter.ItemProcessorAdapter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.ScriptItemProcessor;
import org.springframework.batch.item.validator.ValidatingItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.Arrays;


@EnableBatchProcessing
@SpringBootApplication
public class BatchApplication {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private UpperCaseNameService upperCaseNameService;


    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("job")
                .start(copyFileStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
                .<Customer2, Customer2>chunk(5)
                .reader(customerItemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }


    //////////////////////////// STEP 1 ////////////////////////////

    @Bean
    @StepScope
    public FlatFileItemReader<Customer2> customerItemReader() {
        Resource inputFile = new ClassPathResource("input/customer.csv");

        return new FlatFileItemReaderBuilder<Customer2>()
                .name("customerItemReader")
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
    public ItemWriter<Customer2> itemWriter() {
        return (items) -> items.forEach(System.out::println);
    }

    @Bean
    public UniqueLastNameValidator validator() {
        UniqueLastNameValidator uniqueLastNameValidator = new UniqueLastNameValidator();

        uniqueLastNameValidator.setName("validator");

        return uniqueLastNameValidator;
    }

    @Bean
    public ValidatingItemProcessor<Customer2> customerValidatingItemProcessor() {
        ValidatingItemProcessor<Customer2> itemProcessor = new ValidatingItemProcessor<>(validator());

        itemProcessor.setFilter(true);

        return itemProcessor;
    }

    @Bean
    public ItemProcessorAdapter<Customer2, Customer2> upperCaseItemProcessor(
            UpperCaseNameService service) {
        ItemProcessorAdapter<Customer2, Customer2> adapter = new ItemProcessorAdapter<>();

        adapter.setTargetObject(service);
        adapter.setTargetMethod("upperCase");

        return adapter;
    }

    @Bean
    public ScriptItemProcessor<Customer2, Customer2> lowerCaseItemProcessor() {
        Resource script = new ClassPathResource("lowerCase.js");

        ScriptItemProcessor<Customer2, Customer2> itemProcessor = new ScriptItemProcessor<>();

        itemProcessor.setScript(script);

        return itemProcessor;
    }

    @Bean
    public CompositeItemProcessor<Customer2, Customer2> itemProcessor() {
        CompositeItemProcessor<Customer2, Customer2> itemProcessor = new CompositeItemProcessor<>();

        itemProcessor.setDelegates(Arrays.asList(
                customerValidatingItemProcessor(),
                upperCaseItemProcessor(upperCaseNameService),
                lowerCaseItemProcessor()));

        return itemProcessor;
    }

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
