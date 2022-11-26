package com.example.batch;

import com.example.batch.incrementer.DailyJobTimestamper;
import com.example.batch.listener.JobLoggerListener;
import com.example.batch.service.CustomService;
import com.example.batch.step.tasklet.HelloWorld;
import com.example.batch.validator.ParameterValidator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.CompositeJobParametersValidator;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.listener.JobListenerFactoryBean;
import org.springframework.batch.core.step.tasklet.CallableTaskletAdapter;
import org.springframework.batch.core.step.tasklet.MethodInvokingTaskletAdapter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.concurrent.Callable;

@EnableBatchProcessing
@SpringBootApplication
public class BatchApplication {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public CompositeJobParametersValidator validator() {
        CompositeJobParametersValidator validator = new CompositeJobParametersValidator();

        DefaultJobParametersValidator defaultJobParametersValidator = new DefaultJobParametersValidator(
                new String[] {"fileName"},
                new String[] {"name", "currentDate", "message"}
        );

        defaultJobParametersValidator.afterPropertiesSet();

        validator.setValidators(
                Arrays.asList(new ParameterValidator(),
                        defaultJobParametersValidator)
        );

        return validator;
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("basicJob")
                .start(methodInvokingStep())
                .validator(validator())
                .incrementer(new DailyJobTimestamper())
                .listener(JobListenerFactoryBean.getListener(
                        new JobLoggerListener()))
                .build();
    }

    @Bean
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
                .tasklet(new HelloWorld())
                .listener(promotionListener())
                .build();
    }

    @Bean
    public Step callableStep() {
        return this.stepBuilderFactory.get("callableStep")
                .tasklet(tasklet())
                .build();
    }


    @Bean
    public Step methodInvokingStep() {
        return this.stepBuilderFactory.get("methodInvokingStep")
                .tasklet(methodInvokingTasklet(null))
                .build();
    }

    @Bean
    public StepExecutionListener promotionListener() {
        ExecutionContextPromotionListener listener
                = new ExecutionContextPromotionListener();

        listener.setKeys(new String[] {"name"});

        return listener;
    }

    @Bean
    public CallableTaskletAdapter tasklet() {
        CallableTaskletAdapter callableTaskletAdapter =
                new CallableTaskletAdapter();

        callableTaskletAdapter.setCallable(callableObject());

        return callableTaskletAdapter;
    }

    @StepScope
    @Bean
    public MethodInvokingTaskletAdapter methodInvokingTasklet(
            @Value("#{jobParameters['message']}") String message
    ) {
        MethodInvokingTaskletAdapter methodInvokingTaskletAdapter =
                new MethodInvokingTaskletAdapter();

        methodInvokingTaskletAdapter.setTargetObject(service());
        methodInvokingTaskletAdapter.setTargetMethod("serviceMethod");
        methodInvokingTaskletAdapter.setArguments(new String[] {message});

        return methodInvokingTaskletAdapter;
    }

    @Bean
    public Callable<RepeatStatus> callableObject() {
        return () -> {
            System.out.println("This was executed in another thread");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public CustomService service() {
        return new CustomService();
    }

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }

}
