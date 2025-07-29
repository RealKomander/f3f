plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.dristmine"
version = "1.1.0"
base.archivesName = "f3f-bukkit"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/") // PacketEvents
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // FIXED: Use latest stable PacketEvents version
    compileOnly("com.github.retrooper:packetevents-spigot:2.4.0")

    // LuckPerms
    compileOnly("net.luckperms:api:5.4")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.release.set(21)
    }
}
