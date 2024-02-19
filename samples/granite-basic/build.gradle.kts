plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.granite)

  application
}

application {
  mainClass = "io.github.darvld.granite.sample.MainKt"
}

granite {
  // example of custom component ID range
  minComponentId = 150
  maxComponentId = 200
}

dependencies {
  implementation(projects.packages.graniteCore)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(kotlin("test-junit"))
}
