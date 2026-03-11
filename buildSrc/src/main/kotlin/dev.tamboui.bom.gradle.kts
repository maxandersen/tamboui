plugins {
    `java-platform`
    id("dev.tamboui.publishing-base")
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            from(components["javaPlatform"])
        }
    }
}

group = "dev.tamboui"

dependencies {
    constraints {
        rootProject.allprojects.forEach { p ->
            p.pluginManager.withPlugin("dev.tamboui.publishing") {
                api(project(p.path))
            }
        }
    }
}
