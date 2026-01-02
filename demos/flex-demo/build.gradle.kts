plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing Flex layouts"

dependencies {
    implementation(projects.tambouiTui)
    implementation(projects.tambouiWidgets)
}

application {
    mainClass.set("dev.tamboui.demo.FlexDemo")
}
