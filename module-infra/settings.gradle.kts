plugins {
    id("org.springframework.boot")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":module-domain"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")
}