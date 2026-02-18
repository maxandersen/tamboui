plugins {
    id("dev.tamboui.demo-project")
}

description = "Interactive thread dump review and comparison dashboard"

demo {
    displayName = "ThreadDump Review"
    tags = setOf("toolkit", "threads", "analysis", "comparison", "monitoring", "list", "tabs", "sparkline")
}

dependencies {
    implementation(projects.tambouiToolkit)
    implementation("me.bechberger:jthreaddump:0.5.8")

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass.set("dev.tamboui.demo.threaddump.ThreadDumpReviewDemo")
}
