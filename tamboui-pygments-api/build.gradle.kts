plugins {
    id("dev.tamboui.java-library")
    `java-test-fixtures`
}

description = "Syntax highlighting API for TamboUI"

dependencies {
    api(projects.tambouiCore)

    testFixturesApi(libs.assertj.core)
}
