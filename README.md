Grails Spring Batch Plugin
====================

This plugin adds the Spring Batch framework to a __Grails 3__ project. 
It's intent is to minimize/eliminate the need for verbose XML files to configure Spring Batch jobs.

### Note: Under development.

## Getting Started

To install the plugin, add the following entry to your `build.groovy` file in the dependencies sections:
```groovy
compile "org.grails.plugins:spring-batch:3.0.0.BUILD-SNAPSHOT"
```
Add `copyBatchConfig` task to your `build.groovy` file at the end:
```groovy
task copyBatchConfig {
    copy {
        from("${project.projectDir}/grails-app/batch") {
            include '**/*BatchConfig.groovy'
        }
        into "${buildDir}/resources/main/batch"
    }
}
```

Once the plugin is installed, you can define your Spring Batch job configuration in a Groovy script file in your application's `grails-app/batch` directory. 
The script's filename must end with BatchConfig (i.e. `SimpleJobBatchConfig.groovy`). 
Define your Spring Batch job using the Grails BeanBuilder syntax (just like in the `resources.groovy` file).

To launch a job from your application do the following:

1. Inject the Spring Batch Job Launcher and the job you defined in your configuration file into the controller or service (or lookup it up from the `grailsApplication.mainContext`)
2. Call the `jobLauncher.run()` method with a reference to your job and a `JobParameters` object. Spring Batch will take care of the rest

`grails-app/batch/SimpleJobBatchConfig.groovy`
```groovy
beans {

    batch.job(id: 'simpleJob') {
        batch.step(id: 'logStart') {
            batch.tasklet(ref: 'printStartMessage')
        }
    }

    printStartMessage(PrintStartMessageTasklet) { bean ->
        bean.autowire = "byName"
    }

}
```

`grails-app/controllers/foo/FooController.groovy`
```groovy
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder

class FooController {
    def jobLauncher
    def simpleJob
    def launch() {
        JobParameters jobParameters = new JobParametersBuilder().addDate("now",
                new Date()).addString("comment", "Unit Test").toJobParameters();
        jobLauncher.run(simpleJob, jobParameters)
        respond ""
    }
}
```


