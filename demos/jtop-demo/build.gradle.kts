plugins {
    id("dev.tamboui.demo-project")
}

description = "JTop - System monitor demo using the DSL module"

dependencies {
    implementation(projects.tambouiToolkit)
    implementation("com.github.oshi:oshi-core:6.9.2")
}

application {
    mainClass.set("dev.tamboui.demo.JTopDemo")
}
