plugins {
    id("org.springframework.boot")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":module-domain"))
    implementation(project(":module-infra"))
    implementation(project(":module-client"))
    implementation(project(":module-common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.redisson:redisson-spring-boot-starter:3.27.0")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")

    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}