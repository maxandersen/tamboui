plugins {
    id("dev.tamboui.demo-project")
}

description = "Rich / BBCode-style markup demo (Text/Line/Span)"

demo {
    displayName = "Markup Demo"
}

dependencies {
    implementation(projects.tambouiCore)
}

application {
    mainClass.set("dev.tamboui.demo.MarkupDemo")
}


