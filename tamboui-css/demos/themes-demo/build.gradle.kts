plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing semantic color system across multiple themes"

demo {
    displayName = "Themes Showcase"
    tags = setOf("css", "themes", "semantic-colors", "markup", "design-tokens")
}

dependencies {
    implementation(projects.tambouiCss)
    implementation(projects.tambouiToolkit)
    implementation(projects.tambouiJline3Backend)
}

application {
    mainClass.set("dev.tamboui.demo.ThemesDemo")
}
