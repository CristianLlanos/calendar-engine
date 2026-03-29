val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val mysql_version: String by project

plugins {
    kotlin("jvm") version "1.9.25"
    id("io.ktor.plugin") version "2.3.12"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.25"
    id("org.jetbrains.dokka") version "1.9.20"
    signing
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "com.cristianllanos"
version = "0.0.1"

application {
    mainClass.set("com.cristianllanos.calendarengine.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

signing {
    useGpgCmd()
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("com.cristianllanos", "calendar-engine", version.toString())

    pom {
        name.set("Calendar Engine")
        description.set("Headless multi-tenant calendar engine with bookings, RFC 5545 recurrence, Google Calendar & CalDAV sync")
        url.set("https://github.com/CristianLlanos/calendar-engine")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("cristianllanos")
                name.set("Cristian Llanos")
                email.set("cristianllanos@outlook.com")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/CristianLlanos/calendar-engine.git")
            developerConnection.set("scm:git:ssh://github.com/CristianLlanos/calendar-engine.git")
            url.set("https://github.com/CristianLlanos/calendar-engine")
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")

    // Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")

    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")

    // MySQL
    implementation("mysql:mysql-connector-java:$mysql_version")

    // DI Container & Event Bus
    implementation("com.cristianllanos:container:0.3.1")
    implementation("com.cristianllanos:events:0.2.1")

    // RRULE (RFC 5545 recurrence)
    implementation("org.dmfs:lib-recur:0.17.1")

    // Ktor HTTP Client (for Google Calendar API, CalDAV)
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")

    // Connection Pool
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("com.h2database:h2:2.2.224")
}
