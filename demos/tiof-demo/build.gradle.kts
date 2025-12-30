plugins {
    id("dev.tamboui.demo-project")
}

description = "TIOF - the Term Is On Fire (Java + TamboUI + OSHI)"

dependencies {
    implementation(projects.tambouiToolkit)
    implementation(libs.oshi.core)
}

application {
    mainClass.set("dev.tamboui.demo.TiofDemo")
}

