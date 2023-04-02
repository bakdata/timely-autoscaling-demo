@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("java")
    alias(libs.plugins.jandex)
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
    implementation(libs.quarkus.avro)
    implementation(libs.quarkus.kafkaStreams)
    implementation(libs.quarkus.kafka.registry)
    implementation((project(":kafka-common")))
    implementation(libs.confluent.avro.serde) {
        exclude(group = "jakarta.ws.rs", module = "jakarta.ws.rs-api")
    }
    implementation(libs.streamsBootstrap) {
        exclude(group = "org.apache.logging.log4j")
    }
}

tasks.test {
    useJUnitPlatform()
}