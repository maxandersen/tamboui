plugins {
    id("dev.tamboui.demo-project")
}

description = "New Year countdown clock with fireworks animation"

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.NewYearCountdownDemo")
}

