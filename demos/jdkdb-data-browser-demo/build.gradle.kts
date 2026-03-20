plugins {
    id("dev.tamboui.demo-project")
}

description = "Interactive browser for the jdkdb-data Java SDK metadata API"

demo {
    displayName = "JDKDB Data Browser"
    tags = setOf("tui", "api", "http", "java", "sdk", "downloads", "list", "search")
}

dependencies {
    implementation(projects.tambouiTui)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.0")
}

application {
    mainClass.set("dev.tamboui.demo.jdkdb.JdkDbDataBrowserDemo")
}
