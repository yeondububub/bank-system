plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21" apply false
    id("org.springframework.boot") version "3.4.3" apply false
    id("io.spring.dependency-management") version "1.1.7"
}

allprojects {
    group = "com"
    version = "0.0.1-SNAPSHOT"
    description = "bank-system"
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.spring.dependency-management")

    dependencyManagement {
        imports { mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES) }
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        testImplementation("org.springframework.boot:spring-boot-starter-test")

        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.test {
        useJUnitPlatform()
    }
}