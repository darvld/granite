rootProject.name = "buildkit"

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
    mavenLocal()
  }
  
  plugins {
    // shared remote cache, see https://docs.less.build/docs/gradle for configuration
    id("build.less") version ("1.0.0-beta6")
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
    mavenLocal()
  }

  // reuse dependency catalog from the parent build
  versionCatalogs.create("libs") {
    from(files("../../gradle/libs.versions.toml"))
  }
}
