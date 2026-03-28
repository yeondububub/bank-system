plugins {
    id("org.springframework.boot")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":module-domain"))
    implementation(project(":module-infra")) // 빈(Bean) 조립을 위해 참조

    implementation("org.springframework.boot:spring-boot-starter-web")
}