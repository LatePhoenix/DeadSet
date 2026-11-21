plugins {
  alias(libs.plugins.kotlin.jvm)
  application
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
  jvmToolchain(11)
}

application {
  mainClass.set("com.example.importer.ImporterMainKt")
}

dependencies {
  implementation(project(":shared"))
  testImplementation(libs.junit)
}

tasks.test {
  useJUnit()
}
