plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing TreeWidget"

demo {
    tags = setOf("tree", "widget", "navigation")
}

application {
    mainClass.set("dev.tamboui.demo.TreeWidgetDemo")
}
