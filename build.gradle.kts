import io.qameta.allure.gradle.base.AllureExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("dev.tamboui.parent")
    alias(libs.plugins.allure.aggregate.report)
    alias(libs.plugins.allure.adapter) apply false
}

repositories {
    mavenCentral()
}

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val allureJavaVersionValue = libsCatalog.findVersion("allure").get().requiredVersion
val allureCommandlineVersionValue = libsCatalog.findVersion("allure-commandline").get().requiredVersion

allprojects {
    repositories {
        mavenCentral()
    }
    apply(plugin = "io.qameta.allure-adapter")

    plugins.withId("io.qameta.allure-adapter") {
        extensions.configure<AllureExtension>("allure") {
            version.set(allureCommandlineVersionValue)
            adapter {
                allureJavaVersion.set(allureJavaVersionValue)
            }
        }
    }
}

allure {
    report {
        singleFile.set(true)
        dependsOnTests.set(true)
    }
}

tasks.named("build") {
    dependsOn("allureAggregateReport")
}
