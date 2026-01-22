plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing GraalPy-based syntax highlighting (self-contained, no external Python)"

// GraalPy requires Java 21+
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

demo {
    displayName = "Syntax highlighting (GraalPy)"
    tags = setOf("toolkit", "syntax-highlighting", "graalpy", "richtext")
}

dependencies {
    implementation(projects.tambouiToolkit)
    implementation(projects.tambouiPygmentsGraalpy)
}

application {
    mainClass.set("dev.tamboui.demo.GraalPyDemo")
    // Add flag to suppress Unsafe warnings from GraalPy/Polyglot (JDK 24+)
    applicationDefaultJvmArgs += listOf("--sun-misc-unsafe-memory-access=allow")
}
