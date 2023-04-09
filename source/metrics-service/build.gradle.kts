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
    implementation(libs.quarkus.jib)
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.health)
    implementation(libs.quarkus.avro)
    implementation(libs.quarkus.kafka)
    implementation(libs.quarkus.resteasy.reactive)
    implementation(libs.quarkus.resteasy.reactive.jackson)
    implementation(libs.confluent.avro.serde) {
        exclude(group = "jakarta.ws.rs", module = "jakarta.ws.rs-api")
    }
    implementation("io.quarkus:quarkus-confluent-registry-avro")
    implementation(project(":kafka-common"))


}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
