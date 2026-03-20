plugins {
    id("dev.tamboui.demo-project")
}

description = "code.quarkus.io-style project generator in a TamboUI terminal app"

demo {
    displayName = "Quarkus Project Generator"
    tags = setOf("toolkit", "quarkus", "form", "generator", "application", "extensions")
}

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.quarkus.QuarkusTuiAppDemo")
}
