plugins {
  alias(libs.plugins.kotlinx.abiValidator)
  alias(libs.plugins.kotlin.jvm)
}

kotlin {
  explicitApi()
}

dependencies {
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
}
