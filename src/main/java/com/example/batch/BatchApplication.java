package com.example.batch;

import com.example.batch.config.HibernateBatchConfigurer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.batch.item.data.builder.MongoItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;

import java.util.Collections;
import java.util.Map;


@EnableBatchProcessing
@SpringBootApplication
public class BatchApplication {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;


    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("job")
                .start(copyFileStep())
                .build();
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
                .<Map, Map>chunk(10)
                .reader(tweetsItemReader(null, null))
                .writer(itemWriter())
                .build();
    }


    //////////////////////////// STEP 1 ////////////////////////////

    @Bean
    @StepScope
    public MongoItemReader<Map> tweetsItemReader(MongoOperations mongoTemplate,
                                                 @Value("#{jobParameters['hashTag']}") String hashtag) {

        return new MongoItemReaderBuilder<Map>()
                .name("tweetsItemReader")
                .targetType(Map.class)
                .jsonQuery("{ \"entities.hashtags.text\": { $eq: ?0}}")
                .collection("tweets_collection")
                .parameterValues(Collections.singletonList(hashtag))
                .pageSize(10)
                .sorts(Collections.singletonMap("created_at", Sort.Direction.ASC))
                .template(mongoTemplate)
                .build();
    }

    @Bean
    public ItemWriter<Map> itemWriter() {
        return (items) -> items.forEach(System.out::println);
    }

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
