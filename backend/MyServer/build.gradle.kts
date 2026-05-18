plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.java-websocket:Java-WebSocket:1.5.6")
    implementation("org.json:json:20240303")
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("airwar-server")

    manifest {
        attributes["Main-Class"] = "com.example.myserver.LoginServer"
    }

    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE

    from(
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    )
}