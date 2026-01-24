plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing emoji table with filtering and scrolling"

demo {
    tags = setOf("emoji", "table", "filter", "scroll", "text-input", "toolkit")
}

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.EmojiDemo")
}
