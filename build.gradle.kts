import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val mainPkgAndClass = "com.kaiqkt.{{projectPackage}}.ApplicationKt"
val commonsVersion = "1.2.0"

val excludePackages: List<String> by extra {
    listOf(
        "com/kaiqkt/{{projectPackage}}/generated/application/controllers/**",
        "com/kaiqkt/{{projectPackage}}/generated/application/dto/**",

        "com/kaiqkt/{{projectPackage}}/Application*",
    )
}

@Suppress("UNCHECKED_CAST")
fun ignorePackagesForReport(jacocoBase: JacocoReportBase) {
    jacocoBase.classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(jacocoBase.project.extra.get("excludePackages") as List<String>)
        }
    )
}

group = "com.kaiqkt"
version = "1.0.0"
description = "Spring boot application"

repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/kaiqkt/*")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GPR_API_KEY")
        }
    }
}

plugins {
    application
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.spring") version "1.6.21"
    id("org.springframework.boot") version "2.7.1"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("io.gitlab.arturbosch.detekt") version "1.19.0"
    id("org.openapi.generator") version "5.1.1"
    id("jacoco")
}

sourceSets {
    create("componentTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val componentTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

configurations["componentTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

dependencies {
    //kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    //commons
    implementation("com.kaiqkt:commons-spring-security:$commonsVersion")
    implementation("com.kaiqkt:commons-spring-health-checker:$commonsVersion")

    //swagger
    implementation("org.springdoc:springdoc-openapi-ui:1.6.14")

    //spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")

    //fuel
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")

    //utils
    implementation("io.azam.ulidj:ulidj:1.0.1")
    implementation("com.github.ua-parser:uap-java:1.5.3")

    //jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")

    testImplementation("com.ninja-squad:springmockk:3.1.1")
    testImplementation("org.mock-server:mockserver-netty:5.11.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    componentTestImplementation("org.mock-server:mockserver-netty:5.11.2")
    componentTestImplementation("org.springframework.boot:spring-boot-starter-test")
    componentTestImplementation(sourceSets["test"].output)
}

application {
    mainClass.set(mainPkgAndClass)
}

jacoco {
    toolVersion = "0.8.7"
    reportsDirectory.set(layout.buildDirectory.dir("jacoco"))
}

detekt {
    source = files("src/main/java", "src/main/kotlin")
    config = files("detekt/detekt.yml")
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    skipValidateSpec.set(true)
    inputSpec.set("$rootDir/src/main/resources/static/api-docs.yml")
    outputDir.set("$buildDir/generated/")
    configFile.set("$rootDir/src/main/resources/static/api-config.json")
}

java.sourceSets["main"].java.srcDir("$buildDir/generated/src/main/kotlin")

tasks.withType<KotlinCompile> {
    java.sourceCompatibility = JavaVersion.VERSION_11
    java.targetCompatibility = JavaVersion.VERSION_11

    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

springBoot {
    buildInfo()
}

tasks.withType<CreateStartScripts> { mainClass.set(mainPkgAndClass) }

tasks.jar {
    enabled = false
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes("Main-Class" to mainPkgAndClass)
        attributes("Package-Version" to archiveVersion)
    }

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    from(sourceSets.main.get().output)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val componentTestTask = tasks.create("componentTest", Test::class) {
    description = "Run the component tests."
    group = "verification"

    testClassesDirs = sourceSets["componentTest"].output.classesDirs
    classpath = sourceSets["componentTest"].runtimeClasspath

    useJUnitPlatform()
}

tasks.withType<JacocoReport> {
    reports {
        xml.required
        html.required
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
    ignorePackagesForReport(this)
}

tasks.withType<JacocoCoverageVerification> {
    violationRules {
        rule {
            limit {
                minimum = "1.0".toBigDecimal()
                counter = "LINE"
            }
            limit {
                minimum = "1.0".toBigDecimal()
                counter = "BRANCH"
            }
        }
    }
    ignorePackagesForReport(this)
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification, componentTestTask)
}