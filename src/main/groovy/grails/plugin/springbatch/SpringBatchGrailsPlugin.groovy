package grails.plugin.springbatch

import grails.plugins.*

class SpringBatchGrailsPlugin extends Plugin {

   // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.0.0.M1 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Grails Spring Batch Plugin"
    def author = "Sumanth Chinthagunta"
    def authorEmail = "xmlking@gmail.com"
    def description = '''\
 Spring Batch Plugin for Grails 3.
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "https://github.com/xmlking/grails3-spring-batch"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]
    def issueManagement = [system: "GitHub", url: "https://github.com/xmlking/grails3-spring-batch/issues"]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]
    def scm = [url: "https://github.com/xmlking/grails3-spring-batch"]

    Closure doWithSpring() { {->
            // TODO Implement runtime spring config (optional)
        } 
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
