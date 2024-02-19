package io.github.darvld.granite.gradle

import io.github.darvld.granite.gradle.BuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Plugin used to configure the Granite compiler plugin.
 *
 * The [GraniteExtension] is registered by this plugin and can be used to configure generation options for synthetic
 * component types.
 *
 * @see GraniteExtension
 */
@Suppress("unused") class GranitePlugin : KotlinCompilerPluginSupportPlugin {
  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = BuildConfig.KOTLIN_PLUGIN_GROUP,
    artifactId = BuildConfig.KOTLIN_PLUGIN_NAME,
    version = BuildConfig.KOTLIN_PLUGIN_VERSION
  )

  override fun getCompilerPluginId(): String {
    // plugin ID is set at build-time (see build.gradle.kts for details)
    return BuildConfig.KOTLIN_PLUGIN_ID
  }

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
    // always apply the plugin
    return true
  }

  override fun apply(target: Project): Unit = with(target) {
    // register the extension for configuration
    extensions.create(GraniteExtension.EXTENSION_NAME, GraniteExtension::class.java)
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.getByType(GraniteExtension::class.java)

    return project.provider {
      // apply plugin options from the extension properties
      val minId = extension.minComponentId.asSupluginOption("min-component-id")
      val maxId = extension.maxComponentId.asSupluginOption("max-component-id")

      listOfNotNull(minId, maxId)
    }
  }

  private fun Property<*>.asSupluginOption(option: String): SubpluginOption? {
    return this.orNull?.let { SubpluginOption(option, it.toString()) }
  }
}