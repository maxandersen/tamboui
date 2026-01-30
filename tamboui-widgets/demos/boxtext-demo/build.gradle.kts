plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the BoxText widget"

demo {
    tags = setOf("boxtext", "text", "block", "colors")
}

application {
    mainClass.set("dev.tamboui.demo.BoxTextDemo")
}

