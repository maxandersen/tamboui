plugins {
    id("dev.tamboui.java-library")
}

description = "OpenRewrite recipes for TamboUI API migrations."

dependencies {
    implementation(platform(libs.rewrite.bom))
    implementation(libs.rewrite.java)

    testImplementation(project(":tamboui-core"))
    testImplementation(project(":tamboui-tui"))

    testImplementation(libs.rewrite.test) {
        exclude(group = "org.slf4j", module = "slf4j-nop")
    }
    testRuntimeOnly("org.openrewrite:rewrite-java-25")
}
