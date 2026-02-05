plugins {
    id("dev.tamboui.demo-project")
}

description = "TextVM demo inspired by VisualVM process introspection"

demo {
    displayName = "TextVM VisualVM Demo"
    tags = setOf("toolkit", "css", "jmx", "monitoring", "processes")
}

dependencies {
    implementation(projects.tambouiToolkit)
    implementation(projects.tambouiCss)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
}

application {
    mainClass = "dev.tamboui.demo.textvm.TextVmDemo"
}
