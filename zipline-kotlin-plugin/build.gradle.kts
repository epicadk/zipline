import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.plugins.JavaPlugin.TEST_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME

plugins {
  kotlin("jvm")
  kotlin("kapt")
  id("com.github.gmazzo.buildconfig")
  id("com.vanniktech.maven.publish.base")
}

dependencies {
  compileOnly(kotlin("compiler-embeddable"))
  compileOnly(kotlin("stdlib"))

  kapt(libs.auto.service.compiler)
  compileOnly(libs.auto.service.annotations)
  api(libs.okio.core)

  testImplementation(projects.zipline)
  testImplementation(libs.assertk)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test)
  testImplementation(kotlin("compiler-embeddable"))
}

// In order to simplify writing test schemas, inject the test sources and
// test classpath as properties into the test runtime. This allows testing
// the FIR-based parser on sources written inside the test case. Cool!
tasks.named<Test>(TEST_TASK_NAME).configure {
  val compilation = kotlin.target.compilations.getByName(TEST_COMPILATION_NAME)

  val sources = compilation.defaultSourceSet.kotlin.sourceDirectories.files
  systemProperty("zipline.internal.sources", sources.joinToString(separator = File.pathSeparator))

  val classpath = project.configurations.getByName(compilation.compileDependencyConfigurationName).files
  systemProperty("zipline.internal.classpath", classpath.joinToString(separator = File.pathSeparator))
}

buildConfig {
  useKotlinOutput {
    internalVisibility = true
  }

  packageName("app.cash.zipline.kotlin")
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${libs.plugins.zipline.kotlin.get()}\"")
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(
      javadocJar = JavadocJar.Empty()
    )
  )
}
