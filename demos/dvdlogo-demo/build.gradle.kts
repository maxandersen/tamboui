plugins {
    id("dev.tamboui.demo-project")
}

description = "DVD logo screensaver demo – bouncing logo with color changes on each bounce"

demo {
    displayName = "DVD Logo"
    tags = setOf("animation", "screensaver", "fun", "canvas")
    internal = true
}

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.dvdlogo.DVDLogoDemo")
}
