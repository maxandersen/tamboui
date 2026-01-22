plugins {
    id("dev.tamboui.java-library")
    `java-test-fixtures`
    id("org.graalvm.python") version "25.0.1"
}

description = "GraalPy-based syntax highlighting (self-contained, no external Python required)"

// GraalPy requires Java 21+
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    // Remove -Werror as GraalVM libs may have warnings
    options.compilerArgs.remove("-Werror")
}

// Configure GraalPy to install pygments
graalPy {
    packages = setOf("pygments==2.19.2")
}

dependencies {
    api(projects.tambouiPygmentsApi)
    // GraalPy plugin auto-injects polyglot and python-embedding dependencies

    testFixturesApi(libs.assertj.core)
}
