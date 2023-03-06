@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("java")
    alias(libs.plugins.quarkus)
    alias(libs.plugins.lombok)
}

group = "com.bakdata"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.platform))
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.reasteasy.reactive.jackson)
    implementation(libs.quarkus.kafka)
    implementation(libs.quarkus.apicurioAvro)
    implementation((project(":kafka-common")))
    testImplementation("io.quarkus:quarkus-test-kafka-companion")
    testImplementation(libs.quarkus.junit)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
