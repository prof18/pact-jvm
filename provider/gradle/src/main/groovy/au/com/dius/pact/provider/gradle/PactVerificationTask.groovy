package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderVerifier
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleScriptException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

import javax.inject.Inject

/**
 * Task to verify a pact against a provider
 */
abstract class PactVerificationTask extends PactVerificationBaseTask {
  @Internal
  IProviderVerifier verifier = new ProviderVerifier()
  @Internal
  GradleProviderInfo providerToVerify

    @Inject
    protected abstract ProviderFactory getProviderFactory();

    @Input
    @Optional
    abstract ListProperty<URL> getTestClasspathURL()

//    @Input
//    abstract Property<TaskContainer> getTaskContainer()
//
    @Input
    abstract SetProperty<Task> getTaskContainer()

//    @Input
//    abstract NamedDomainObjectSet<Task> getTasksContainerSet()

//    @Input
//    abstract Provider<TaskContainer> getTaskContainer()

  @Input
   TaskProvider<Task> stateChangeTask


  @TaskAction
  void verifyPact() {

//      GradleBuild task = stateChangeTask.get()
//      task.tasks = ['canIDeploy']
//    task.execute()
    stateChangeTask.configure {
        finalizedBy("canIDeploy")
    }
//      stateChangeTask.get().execute()

//      taskContainer.get().find {it.name == "canIDeploy" }.execute()


      // set here?

    verifier.with {

      verificationSource = 'gradle'
      projectHasProperty = { providerFactory.gradleProperty(it).present }
      projectGetProperty = { providerFactory.gradleProperty(it).get() }
      pactLoadFailureMessage = { 'You must specify the pact file to execute (use pactSource = file(...) etc.)' }
      checkBuildSpecificTask = { it instanceof Task || it instanceof String && taskCOllection.find { t -> t.name == it } }
      executeBuildSpecificTask = this.&executeStateChangeTask
      projectClasspath = {
        testClasspathURL.get()
      }
        providerVersion = providerToVerify.providerVersion ?: { project.version }
      if (providerToVerify.providerTags) {
        if (providerToVerify.providerTags instanceof Closure ) {
          providerTags = providerToVerify.providerTags
        } else if (providerToVerify.providerTags instanceof List) {
          providerTags = { providerToVerify.providerTags }
        } else if (providerToVerify.providerTags instanceof String) {
          providerTags = { [ providerToVerify.providerTags ] }
        } else {
          throw new GradleScriptException(
            "${providerToVerify.providerTags} is not a valid value for providerTags", null)
        }
      }

      if (project.pact.reports) {
        def reportsDir = new File(project.buildDir, 'reports/pact')
        reporters = project.pact.reports.toVerifierReporters(reportsDir, it)
      }
    }

    if (providerToVerify.consumers.empty && !ignoreNoConsumers()) {
      throw new GradleScriptException("There are no consumers for service provider '${providerToVerify.name}'", null)
    }

    runVerification(verifier, providerToVerify)
  }

  def executeStateChangeTask(t, state) {
    def taskSet = taskContainer.get()
    def task = t instanceof String ? taskSet.find {it.name == t } : t
    task.setProperty('providerState', state)
    task.ext.providerState = state

    //    def build = project.task(type: GradleBuild) {
      //      tasks = [task.name]
      //    }

      task.


    def build = taskSet.find {
      it instanceof GradleBuild
    } as GradleBuild
    build.tasks = [task.name]
    build.execute()
  }

//    def executeStateChangeTask(t, state) {
//        def task = t instanceof String ? project.tasks.getByName(t) : t
//        task.setProperty('providerState', state)
//        task.ext.providerState = state
//        def build = project.task(type: GradleBuild) {
//            tasks = [task.name]
//        }
//        build.execute()
//    }

}
