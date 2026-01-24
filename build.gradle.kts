import io.qameta.allure.gradle.base.AllureExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("dev.tamboui.parent")
    alias(libs.plugins.allure.aggregate.report)
    alias(libs.plugins.allure.adapter) apply false
}

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val allureJavaVersion = libsCatalog.findVersion("allure").get().requiredVersion
val allureCommandlineVersion = libsCatalog.findVersion("allure-commandline").get().requiredVersion

allprojects {
    apply(plugin = "io.qameta.allure-adapter")

    plugins.withId("io.qameta.allure-adapter") {
        extensions.configure<AllureExtension>("allure") {
            version.set(allureCommandlineVersion)
            adapter {
                allureJavaVersion.set(allureJavaVersion)
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
