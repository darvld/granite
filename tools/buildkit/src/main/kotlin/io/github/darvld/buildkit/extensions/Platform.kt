package io.github.darvld.buildkit.extensions

import io.github.darvld.buildkit.extensions.Architecture.AMD64
import io.github.darvld.buildkit.extensions.Architecture.ARM64
import io.github.darvld.buildkit.extensions.OperatingSystem.*
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.configurationcache.extensions.capitalized

/** Represents an OS family for the purposes of platform-specific dependency resolution. */
public enum class OperatingSystem {
  LINUX,
  MACOS,
  WINDOWS,
}

/** Represents an OS architecture for the purposes of platform-specific dependency resolution. */
public enum class Architecture {
  AMD64,
  ARM64,
}

/** Configures platform-specific source sets and related features. */
public interface PlatformExtension {
  public companion object {
    internal const val NAME = "platformDependencies"

    /** Resolve the current [OperatingSystem] using the `os.name` system property. */
    public fun currentOS(): OperatingSystem {
      val os = System.getProperty("os.name").lowercase()
      return when {
        os.contains("linux") -> LINUX
        os.contains("mac") -> MACOS
        os.contains("windows") -> WINDOWS
        else -> error("Unsupported operating system: $os")
      }
    }

    /** Resolve the current [Architecture] using the `os.arch` system property. */
    public fun currentArch(): Architecture {
      val arch = System.getProperty("os.arch").lowercase()
      return when {
        arch.contains("x86_64") || arch.contains("amd64") -> AMD64
        arch.contains("aarch64") || arch.contains("arm64") -> ARM64
        else -> error("Unsupported architecture: $arch")
      }
    }
  }
}

/**
 * Register and configure the [PlatformExtension] for this project. The extension will only be applied if the Java
 * plugin is present. Custom platform-specific configurations will be added and registered according to the current
 * [OperatingSystem] and [Architecture].
 *
 * @see PlatformExtension
 */
internal fun Project.configurePlatformFeatures() {
  // add the extension only if the Java plugin is present
  val java = extensions.findByType(JavaPluginExtension::class.java) ?: return
  extensions.create(PlatformExtension.NAME, PlatformExtension::class.java)

  // configuration variants added for each source set
  val variants = arrayOf("implementation", "compileOnly", "runtimeOnly")
  val currentOs = PlatformExtension.currentOS()
  val currentArch = PlatformExtension.currentArch()
  
  // for every java source set, register configurations for every os/arch combination
  java.sourceSets.configureEach {
    val sourceSetName = name

    variants.forEach { variant ->
      val variantName = variant.replaceFirstChar { it.uppercase() }

      OperatingSystem.values().forEach { os ->
        val osName = os.name.lowercase().capitalized()

        // add a shared configuration for this OS regardless of the arch
        val shared = configurations.create(configurationNameFor(sourceSetName, variantName, osName))

        Architecture.values().forEach { arch ->
          val archName = arch.name.lowercase().capitalized()

          // add a configuration for this os/arch pair
          configurations.create(configurationNameFor(sourceSetName, variantName, osName, archName)) conf@{
            // extend the shared OS configuration
            extendsFrom(shared)

            // if applicable, have the matching default configuration extend from this one
            if (currentOs == os && currentArch == arch) configurationFor(sourceSetName, variantName).configure {
              extendsFrom(this@conf)
            }
          }
        }
      }
    }
  }
}

private fun Project.configurationNameFor(
  sourceSet: String,
  variant: String,
  os: String = "",
  arch: String = ""
): String {
  return if (sourceSet == "main") "${variant.replaceFirstChar { it.lowercase() }}$os$arch"
  else "$sourceSet$variant$os$arch"
}

private fun Project.configurationFor(sourceSet: String, variant: String): NamedDomainObjectProvider<Configuration> {
  return configurations.named(configurationNameFor(sourceSet, variant, os = "", arch = ""))
}