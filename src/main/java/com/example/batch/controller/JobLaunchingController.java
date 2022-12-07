package com.example.batch.controller;

import com.example.batch.dto.JobLaunchRequest;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobLaunchingController {
    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private ApplicationContext context;

    @PostMapping(path="/run")
    public ExitStatus runJob(@RequestBody JobLaunchRequest request) throws Exception {
        Job job = this.context.getBean(request.getName(), Job.class);

        return this.jobLauncher.run(job, request.getJobParameters())
                .getExitStatus();
    }

}
