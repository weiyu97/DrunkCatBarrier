plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.drunkcat.barrier"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.drunkcat.barrier"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // 统一签名文件：debug / release 都用同一份，避免卸载重装签名冲突。
            // release.jks 与密码均不入库：
            //   - release.jks 已由 .gitignore 排除
            //   - 密码从 local.properties（已被 .gitignore 排除）或环境变量读取，
            //     例如在 local.properties 中追加：
            //       RELEASE_STORE_PASSWORD=你的密码
            //       RELEASE_KEY_PASSWORD=你的密码
            //       RELEASE_KEY_ALIAS=drunckat
            val ksFile = rootProject.file("release.jks")
            if (ksFile.exists()) {
                // 从 local.properties（已被 .gitignore 排除）解析签名密码；
                // 不依赖 java.util.Properties，避免 Gradle Kotlin DSL 脚本类路径问题。
                val localProps = rootProject.file("local.properties").let { lp ->
                    if (lp.exists()) {
                        lp.readLines()
                            .map { it.substringBefore("#").trim() }
                            .filter { it.contains("=") }
                            .associate { it.substringBefore("=").trim() to it.substringAfter("=").trim() }
                    } else {
                        emptyMap()
                    }
                }
                val ksPass = localProps["RELEASE_STORE_PASSWORD"]
                    ?: System.getenv("RELEASE_STORE_PASSWORD")
                val keyPass = localProps["RELEASE_KEY_PASSWORD"]
                    ?: System.getenv("RELEASE_KEY_PASSWORD")
                val alias = localProps["RELEASE_KEY_ALIAS"]
                    ?: System.getenv("RELEASE_KEY_ALIAS") ?: "drunckat"
                if (ksPass != null && keyPass != null) {
                    storeFile = ksFile
                    storePassword = ksPass
                    keyAlias = alias
                    keyPassword = keyPass
                }
            }
        }
    }

    buildTypes {
        release {
            // 关闭代码/资源压缩：R8 会剥离或重命名 Compose 运行时类，
            // 导致动画、状态丢失。本项目体积优先让位于功能完整。
            isMinifyEnabled = false
            isShrinkResources = false
            val ksFile = rootProject.file("release.jks")
            if (ksFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            // debug 也用统一签名，避免安装同包名 release 包时签名不一致导致卸载。
            // 注意：保留 applicationIdSuffix = ".debug" 会变成
            // com.drunkcat.barrier.debug，和 release 的包名不同，
            // 也就不能覆盖安装——这里去掉后缀，让两个构建输出同包名。
            val ksFile = rootProject.file("release.jks")
            if (ksFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    // 可选地图底图（OSM，开源，无需 API Key）
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
