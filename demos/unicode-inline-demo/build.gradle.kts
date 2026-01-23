plugins {
    id("dev.tamboui.demo-project")
}

description = "Unicode demo using inline display"

demo {
    displayName = "Unicode wide character demo using inline display"
    internal = true
}

application {
    mainClass.set("dev.tamboui.demo.UnicodeDemo")
}
