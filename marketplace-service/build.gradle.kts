plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    jacoco
}
val basePath = "com/thiagosena/marketplace"
extra["springCloudVersion"] = "2025.1.1"
group = "com.thiagosena"
version = "1.0.0"
description = "Receiver Management"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

ktlint {
    version.set("1.8.0")
}

tasks.test {
    finalizedBy("jacocoTestReport")
}

tasks.build {
    finalizedBy("jacocoTestCoverageVerification", "integrationTest", "archTest")
}

// Detekt
tasks.detekt {
    dependsOn("ktlintCheck")
}
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html {
            required.set(true)
            outputLocation.set(file("build/reports/detekt/detekt.html"))
        }
    }
}
detekt {
    source.from(files("src/main"))
    config.from(files("detekt-config.yml"))
    buildUponDefaultConfig = true
}

// Integration Test
sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}
val integrationTestTask =
    tasks.register("integrationTest", Test::class.java) {
        description = "Runs the integration tests."
        group = "verification"

        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
    }
val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
    extendsFrom(configurations.testImplementation.get())
}

configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

// Arch Test
sourceSets {
    create("archTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}
val archTest =
    tasks.register("archTest", Test::class.java) {
        description = "Runs the architecture tests."
        group = "verification"

        testClassesDirs = sourceSets["archTest"].output.classesDirs
        classpath = sourceSets["archTest"].runtimeClasspath
    }
val archTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
    extendsFrom(configurations.testImplementation.get())
}

configurations["archTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

jacoco {
    toolVersion = "0.8.13"
}

val excludePackages: Iterable<String> =
    listOf(
        "**/$basePath/application/**",
        "**/$basePath/domain/entities/**",
        "**/$basePath/MarketplaceApplication*",
    )
extra["excludePackages"] = excludePackages

tasks.withType<Test> {
    loadEnv(environment, file("variables.test.env"))
}

fun loadEnv(
    environment: MutableMap<String, Any>,
    file: File,
) {
    require(file.exists())

    file.readLines().forEach { line ->
        if (line.isBlank() || line.startsWith("#")) return@forEach

        line
            .split("=", limit = 2)
            .takeIf { it.size == 2 && it[0].isNotBlank() }
            ?.run { Pair(this[0].trim(), this[1].trim()) }
            ?.run { environment[first] = second }
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(excludePackages)
        },
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = 0.9.toBigDecimal()
                counter = "Line"
            }
        }
        rule {
            limit {
                minimum = 0.8.toBigDecimal()
                counter = "BRANCH"
            }
        }
    }
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(excludePackages)
        },
    )
}

val springdocVersion = "3.0.1"
val tngTechArchUnitVersion = "1.4.1"
val feignVersion = "13.7"
val mockkVersion = "1.14.9"
val awaitilityVersion = "4.3.0"
val restAssuredVersion = "6.0.0"
dependencies {
    // Kotlin adapters
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Web
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Observability
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Unit Test
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation(kotlin("test"))

    // Arch Test
    archTestImplementation("com.tngtech.archunit:archunit-junit5-api:$tngTechArchUnitVersion")
    archTestImplementation("com.tngtech.archunit:archunit-junit5-engine:$tngTechArchUnitVersion")

    // Integration Test
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-webflux")
    integrationTestImplementation("org.awaitility:awaitility-kotlin:$awaitilityVersion")
    integrationTestImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
    integrationTestImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
    integrationTestImplementation("io.rest-assured:json-path:$restAssuredVersion")
    integrationTestImplementation("io.rest-assured:kotlin-extensions:$restAssuredVersion")
    integrationTestImplementation("org.springframework.boot:spring-boot-testcontainers")
    integrationTestImplementation("org.testcontainers:junit-jupiter")
    integrationTestImplementation("org.testcontainers:postgresql")
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("org.testcontainers:testcontainers-bom:1.19.8")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("api.version", "1.44")
}

springBoot { buildInfo() }
