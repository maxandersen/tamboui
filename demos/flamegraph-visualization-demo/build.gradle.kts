plugins {
    id("dev.tamboui.demo-project")
}

description = "Flamegraph visualization demo for profiling data"

dependencies {
    implementation(projects.tambouiToolkit)
    runtimeOnly(projects.tambouiJline3Backend)
}

application {
    mainClass.set("dev.tamboui.demo.flamegraph.FlameGraphDemo")
}

demo {
    displayName = "Flamegraph Visualization Demo"
    tags = setOf("profiling", "flamegraph", "performance")
}
