plugins {
    id("dev.tamboui.demo-project")
}

description = "Unicode wide character rendering demo"

demo {
    displayName = "Unicode wide character rendering demo"
    internal = true
}

application {
    mainClass.set("dev.tamboui.demo.UnicodeDemo")
}
