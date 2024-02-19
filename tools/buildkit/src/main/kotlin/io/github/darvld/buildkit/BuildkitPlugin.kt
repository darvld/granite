package io.github.darvld.buildkit

import io.github.darvld.buildkit.extensions.configurePlatformFeatures
import org.gradle.api.Plugin
import org.gradle.api.Project

/** A plugin providing several extensions for a Gradle [Project], used in multiple subprojects. */
public class BuildkitPlugin : Plugin<Project> {
  override fun apply(target: Project): Unit = with(target) {
    // register extensions and features
    if (optionEnabled("platform-configurations")) configurePlatformFeatures()
  }
}
