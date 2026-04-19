plugins {
    id("dev.tamboui.demo-project")
}

description = "Web-accessible demo chooser using Aesh HTTP/WebSocket backend"

demo {
    displayName = "Aesh Web Chooser"
    tags = setOf("aesh", "http", "websocket", "toolkit", "network", "chooser")
    internal = true
}

configurations.all {
    resolutionStrategy {
        // aesh-terminal-http requires Netty 4.1.x; Netty 4.2 removed APIs it uses
        force("io.netty:netty-common:4.1.81.Final")
        force("io.netty:netty-buffer:4.1.81.Final")
        force("io.netty:netty-transport:4.1.81.Final")
        force("io.netty:netty-handler:4.1.81.Final")
        force("io.netty:netty-codec:4.1.81.Final")
        force("io.netty:netty-codec-http:4.1.81.Final")
        force("io.netty:netty-resolver:4.1.81.Final")
        force("io.netty:netty-transport-native-unix-common:4.1.81.Final")
    }
}

dependencies {
    implementation(projects.tambouiToolkit)
    implementation(projects.tambouiAeshBackend)
    implementation(projects.tambouiDemos)
    implementation(libs.aesh.terminal.http)

    // Netty 4.1.x - must match aesh-terminal-http's expected version
    implementation("io.netty:netty-all:4.1.81.Final")
}

application {
    mainClass.set("dev.tamboui.demo.aesh.AeshWebChooserDemo")
}
