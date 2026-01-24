import io.qameta.allure.gradle.base.AllureExtension

plugins {
    id("dev.tamboui.parent")
    alias(libs.plugins.allure.aggregate.report)
    alias(libs.plugins.allure.adapter) apply false
}

val allureJavaVersion = libs.versions.allure.get()
val allureCommandlineVersion = libs.versions.allure.commandline.get()

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
