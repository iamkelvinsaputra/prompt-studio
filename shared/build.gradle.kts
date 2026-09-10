import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework { baseName = "PromptStudio"; isStatic = true; binaryOption("bundleId", "com.kelvinsaputra.promptstudio.shared") }
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser { testTask { useKarma { useChromeHeadless() } } }; binaries.executable() }
    applyDefaultHierarchyTemplate()
    
    android {
       namespace = "com.kelvinsaputra.promptstudio.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        val nativeGenerationMain by creating {
            dependsOn(commonMain.get())
            dependencies { implementation(libs.ktor.client.core) }
        }
        val nativeGenerationTest by creating {
            dependsOn(commonTest.get())
            dependencies { implementation(libs.ktor.client.mock) }
        }
        val jvmStorageMain by creating { dependsOn(commonMain.get()) }
        jvmMain.get().dependsOn(jvmStorageMain)
        androidMain.get().dependsOn(jvmStorageMain)
        jvmMain.get().dependsOn(nativeGenerationMain)
        androidMain.get().dependsOn(nativeGenerationMain)
        iosMain.get().dependsOn(nativeGenerationMain)
        jvmTest.get().dependsOn(nativeGenerationTest)
        getByName("androidHostTest").dependsOn(nativeGenerationTest)
        iosTest.get().dependsOn(nativeGenerationTest)
        iosMain.dependencies { implementation("io.ktor:ktor-client-darwin:3.4.0") }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.ui.test.junit4)
        }
        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
