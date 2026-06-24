import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

// Load GEMINI_API_KEY robustly across multiple possible sources
val geminiApiKey: String = run {
  // 1. Try system environment variable (CI/CD or server-side injection)
  var key = System.getenv("GEMINI_API_KEY")
  if (!key.isNullOrBlank()) return@run key

  // 2. Try .env file (standard for AI Studio platform Secrets Panel)
  val envFile = rootProject.file(".env")
  if (envFile.exists()) {
    val envProps = Properties()
    envFile.inputStream().use { envProps.load(it) }
    key = envProps.getProperty("GEMINI_API_KEY")
    if (!key.isNullOrBlank()) return@run key
  }

  // 3. Try local.properties file (standard local override)
  val localPropsFile = rootProject.file("local.properties")
  if (localPropsFile.exists()) {
    val localProps = Properties()
    localPropsFile.inputStream().use { localProps.load(it) }
    key = localProps.getProperty("GEMINI_API_KEY")
    if (!key.isNullOrBlank()) return@run key
  }

  // 4. Try .env.example file as absolute last resort fallback
  val envExampleFile = rootProject.file(".env.example")
  if (envExampleFile.exists()) {
    val envExampleProps = Properties()
    envExampleFile.inputStream().use { envExampleProps.load(it) }
    key = envExampleProps.getProperty("GEMINI_API_KEY")
    if (!key.isNullOrBlank()) return@run key
  }

  ""
}

// Load GOOGLE_BOOKS_API_KEY robustly
val googleBooksApiKey: String = run {
  // 1. Try system environment variable
  var key = System.getenv("GOOGLE_BOOKS_API_KEY")
  if (!key.isNullOrBlank()) return@run key

  // 2. Try .env file
  val envFile = rootProject.file(".env")
  if (envFile.exists()) {
    val envProps = Properties()
    envFile.inputStream().use { envProps.load(it) }
    key = envProps.getProperty("GOOGLE_BOOKS_API_KEY")
    if (!key.isNullOrBlank()) return@run key
  }

  // 3. Try local.properties file
  val localPropsFile = rootProject.file("local.properties")
  if (localPropsFile.exists()) {
    val localProps = Properties()
    localPropsFile.inputStream().use { localProps.load(it) }
    key = localProps.getProperty("GOOGLE_BOOKS_API_KEY")
    if (!key.isNullOrBlank()) return@run key
  }

  // 4. Try .env.example
  val envExampleFile = rootProject.file(".env.example")
  if (envExampleFile.exists()) {
    val envExampleProps = Properties()
    envExampleFile.inputStream().use { envExampleProps.load(it) }
    key = envExampleProps.getProperty("GOOGLE_BOOKS_API_KEY")
    if (!key.isNullOrBlank()) return@run key
  }

  // 5. Fallback to geminiApiKey
  if (geminiApiKey.isNotEmpty()) return@run geminiApiKey

  ""
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.kidsbookjournal.fwyksq"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // [요구사항 1] defaultConfig 내부에 GEMINI_API_KEY buildConfigField 추가
    buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    buildConfigField("String", "GOOGLE_BOOKS_API_KEY", "\"$googleBooksApiKey\"")
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // [요구사항 5] R8 ProGuard를 활성화하여 컴파일된 자바 바이트코드를 난독화하고 미사용 리소스/코드를 제거합니다.
      // 실제 프로덕션 릴리즈 환경에서는 아래 값을 true로 변경합니다.
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      
      /*
       * 💡 Senior 클라이언트 엔지니어의 디컴파일 방지 및 바이너리 보안 제언:
       * 
       * 1. minifyEnabled = true 설정을 통해 R8 난독화 파이프라인을 온전히 태웁니다.
       * 2. R8 최적화 과정에서 BuildConfig 클래스 및 평문 API Key가 노출되는 것을 추가적으로 막기 위해,
       *    상수 암호화 플러그인(예: StringFog, DexGuard)을 적용할 수 있습니다.
       *    - StringFog 적용 예시:
       *      buildscript { dependencies { classpath("io.github.megunno:stringfog-gradle-plugin:x.y.z") } }
       *      plugins { id("stringfog") }
       *      stringfog { key = "YourSecretKeyForStringEncryption" }
       * 3. Native C++ 레이어(JNI)에 중요한 API Key를 탑재하고, JNI 호출 시 시그니처 체크를 수행하여
       *    디컴파일 시 메모리 및 바이너리 평문 덤프를 원천적으로 어렵게 만듭니다.
       * 4. proguard-rules.pro 파일 내에 BuildConfig 클래스의 난독화 예외(-keep) 설정을 해제하고,
       *    특정 민감 비즈니스 로직 클래스들을 집중적으로 혼란(obfuscation)시키는 규칙을 적용합니다.
       */
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  // Ignore GEMINI_API_KEY to prevent duplicate field generation
  ignoreList.add("GEMINI_API_KEY")
  ignoreList.add("GOOGLE_BOOKS_API_KEY")
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.exifinterface)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
