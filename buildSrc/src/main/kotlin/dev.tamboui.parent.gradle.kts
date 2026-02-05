import dev.tamboui.build.SplitPackageCheckTask
import dev.tamboui.build.UpdateJBangCatalogTask
import dev.tamboui.build.model.DemosModelBuilder
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry

plugins {
    base
    id("dev.tamboui.maven-central-publishing")
}

// Register the Tooling API model builder for demos
serviceOf<ToolingModelBuilderRegistry>()
    .register(DemosModelBuilder())

val splitPackageCheck = tasks.register<SplitPackageCheckTask>("splitPackageCheck") {
    description = "Checks for split packages across library modules"
    group = "verification"

    // Discover all modules by looking for top-level directories with build.gradle.kts
    rootDir.listFiles()
        ?.filter { it.isDirectory && file("${it.name}/build.gradle.kts").exists() }
        ?.forEach { moduleDir ->
            val sourceDir = file("${moduleDir.name}/src/main/java")
            if (sourceDir.exists()) {
                sourceSet(moduleDir.name, fileTree(sourceDir) { include("**/*.java") })
            }
        }

    reportFile.set(layout.buildDirectory.file("reports/split-packages/report.txt"))
}

tasks.named("check") {
    dependsOn(splitPackageCheck)
}

val incomingDemos by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    description = "Configuration to collect demo projects from all modules"
}

dependencies {
    rootProject.allprojects.forEach { p ->
        p.pluginManager.withPlugin("dev.tamboui.demo-project") {
            incomingDemos(project(p.path)) {
                isTransitive = false
            }
        }
    }
}

tasks.register<UpdateJBangCatalogTask>("updateJBangCatalog") {
    description = "Updates the jbang-catalog.json file with discovered demos"
    group = "build"
    inputs.files(incomingDemos)
    projectDir = layout.projectDirectory
    modules =  rootProject.subprojects.map { it.name }
    catalogFile = layout.projectDirectory.file("jbang-catalog.json")
}
