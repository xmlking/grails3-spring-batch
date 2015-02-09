package grails.plugin.springbatch

import grails.plugin.springbatch.springbatchadmin.patch.PatchedSimpleJobServiceFactoryBean
import grails.plugins.Plugin
import groovy.sql.Sql
import org.springframework.batch.core.configuration.support.DefaultJobLoader
import org.springframework.batch.core.configuration.support.MapJobRegistry
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao
import org.springframework.batch.core.launch.support.SimpleJobLauncher
import org.springframework.batch.core.launch.support.SimpleJobOperator
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.SyncTaskExecutor

import java.sql.Connection
import java.sql.Statement

class SpringBatchGrailsPlugin extends Plugin {

   // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.0.0.M1 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Grails Spring Batch Plugin"
    def author = "Sumanth Chinthagunta"
    def authorEmail = "xmlking@gmail.com"
    def description = '''\
 Spring Batch Plugin for Grails 3.
'''
    def profiles = ['web']
    def documentation = "https://github.com/xmlking/grails3-spring-batch"
    def version = '3.0.0.BUILD-SNAPSHOT'
//    def license = "APACHE" // License: one of 'APACHE', 'GPL2', 'GPL3'
    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]
    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]
    def issueManagement = [system: "GitHub", url: "https://github.com/xmlking/grails3-spring-batch/issues"]
    def scm = [url: "https://github.com/xmlking/grails3-spring-batch"]

    def watchedResources = [
            "file:./grails-app/batch/**/*BatchConfig.groovy",
            "file:./plugins/*/grails-app/batch/**/*BatchConfig.groovy",
    ]

    Closure doWithSpring() { {->

        def application = grailsApplication
        def batchConfig = application.config.grails.springBatch

        batchConfig.tablePrefix = batchConfig.tablePrefix?: "BATCH"
        batchConfig.dataSource = batchConfig.dataSource?: "dataSource"
        batchConfig.'maxVarCharLength' = batchConfig.'maxVarCharLength'?: AbstractJdbcBatchMetadataDao.DEFAULT_EXIT_MESSAGE_LENGTH
        batchConfig.'loadTables'  = batchConfig.'loadTables'?: false
        batchConfig.'database' = batchConfig.'database'?: 'h2'

        String tablePrefix = batchConfig.tablePrefix ? (batchConfig.tablePrefix + '_' ) : ''
        def dataSourceBean = batchConfig.dataSource
        def maxVarCharLength = batchConfig.maxVarCharLength
        def loadRequired = loadRequiredSpringBatchBeans.clone()
        loadRequired.delegate = delegate
        loadRequired(dataSourceBean, tablePrefix, batchConfig.database, maxVarCharLength)
        def loadConfig = loadBatchConfig.clone()
        loadConfig.delegate = delegate
        xmlns batch: "http://www.springframework.org/schema/batch"
        loadConfig()
    } }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext(def applicationContext) {
        def application = applicationContext.grailsApplication
        def conf = application.config.grails.springBatch
        String dataSourceName = conf.dataSource
        def database = conf.database
        def loadTables = conf.loadTables
        if(loadTables) {
            if(database) {
                def ds = applicationContext.getBean(dataSourceName)
                def sql = new Sql(ds)
                sql.withTransaction { Connection conn ->
                    Statement statement = conn.createStatement()
                    def script = "org/springframework/batch/core/schema-drop-${database}.sql"
                    def text = applicationContext.classLoader.getResourceAsStream(script).text
                    text.split(";").each { line ->
                        if(line.trim()) {
                            statement.execute(line.trim())
                        }
                    }

                    script = "org/springframework/batch/core/schema-${database}.sql"
                    text = applicationContext.classLoader.getResourceAsStream(script).text
                    text.split(";").each { line ->
                        if(line.trim()) {
                            statement.execute(line.trim())
                        }
                    }
                    statement.close()
                    conn.commit()
                }
                sql.close()
            } else {
                log.error("Must specify plugin.springBatch.database variable if plugin.springBatch.loadTables = true")
                throw new RuntimeException("Must specify plugin.springBatch.database variable if plugin.springBatch.loadTables = true")
            }
        }
    }

    void onChange(Map<String, Object> event) {
        // Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.

        if(event.source instanceof Class && event.source.name.endsWith("BatchConfig")) {
            Class configClass = event.source
            //Get an instance of the changed class file as a script
            Script script = (Script) configClass.newInstance()
            //Create a new script binding so we can assign a delegate to it
            Binding scriptBinding = new Binding()
            //Allows the script file to delegate the beans DSL to the onChangeEvent
            scriptBinding.beans = { Closure closure ->
                delegate.beans(closure)
            }
            script.binding = scriptBinding
            //Execute the script to get the new beans that were defined
            def beans = script.run()
            //Register new beans into the application context
            beans.registerBeans(event.ctx)
            //This forces the job loader to reload the beans defined in the file that changed
            //This will probably actually reload all spring batch jobs
            def jobLoader = new DefaultJobLoader(event.ctx.jobRegistry)
            jobLoader.reload(new ReloadApplicationContextFactory(event.ctx))
        }
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }

    def loadBatchConfig = { ->
        loadBeans 'classpath*:/batch/*BatchConfig.groovy'
    }

    def loadRequiredSpringBatchBeans = {
        def dataSourceBean, String tablePrefixVal, String dbType, int maxVarCharLengthVal ->

        jobRepository(JobRepositoryFactoryBean) {
            dataSource = ref(dataSourceBean)
            transactionManager = ref("transactionManager")
            tablePrefix = tablePrefixVal
            databaseType = dbType
            maxVarCharLength = maxVarCharLengthVal
            //isolationLevelForCreate = "SERIALIZABLE"
        }

        /*
         * Async launcher to use by default
         */
        jobLauncher(SimpleJobLauncher){
            jobRepository = ref("jobRepository")
            taskExecutor = { SimpleAsyncTaskExecutor executor -> }
        }

        /*
         * Additional Job Launcher to support synchronous scheduling
         */
        syncJobLauncher(SimpleJobLauncher){
            jobRepository = ref("jobRepository")
            taskExecutor = { SyncTaskExecutor executor -> }
        }

        jobExplorer(JobExplorerFactoryBean) {
            dataSource = ref(dataSourceBean)
            tablePrefix = tablePrefixVal
        }
        jobRegistry(MapJobRegistry) { }
        //Use a custom bean post processor that will unregister the job bean before trying to initializing it again
        //This could cause some problems if you define a job more than once, you'll probably end up with 1 copy
        //of the last definition processed instead of getting a DuplicateJobException
        //Had to do this to get reloading to work
        jobRegistryPostProcessor(ReloadableJobRegistryBeanPostProcessor) {
            jobRegistry = ref("jobRegistry")
        }

        jobOperator(SimpleJobOperator) {
            jobRepository = ref("jobRepository")
            jobLauncher = ref("jobLauncher")
            jobRegistry = ref("jobRegistry")
            jobExplorer = ref("jobExplorer")
        }
        jobService(PatchedSimpleJobServiceFactoryBean) {
            jobRepository = ref("jobRepository")
            jobLauncher = ref("jobLauncher")
            jobLocator = ref("jobRegistry")
            dataSource = ref(dataSourceBean)
            tablePrefix = tablePrefixVal
        }
    }
}
