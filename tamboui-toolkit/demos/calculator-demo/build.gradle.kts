plugins {
    id("dev.tamboui.demo-project")
}

description = "Calculator demo (Textual-inspired) using BoxText display and TCSS styling"

demo {
    displayName = "Calculator"
    tags = setOf("toolkit", "css", "calculator", "boxtext", "mouse", "keyboard")
}

dependencies {
    implementation(projects.tambouiToolkit)
    implementation(projects.tambouiWidgets)
}

application {
    mainClass.set("dev.tamboui.demo.CalculatorDemo")
}

