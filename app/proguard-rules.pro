# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# =========================================================================
# [요구사항 3] 상용 배포를 위한 디컴파일 난독화(R8/ProGuard) 조치 및 API Key 보호 규칙
# =========================================================================

# 1. BuildConfig 및 상수 난독화 활성화
# 기본적으로 R8은 최적화(Optimization) 과정에서 BuildConfig의 상수들을 호출부로 인라인(Inline)화하여 흩뿌립니다.
# 특정 공통 라이브러리나 리플렉션용 룰 때문에 BuildConfig 전체가 평문으로 Keep되는 것을 방지해야 합니다.
# 따라서 BuildConfig 전체에 대한 무조건적인 -keep 설정을 하지 않고, 난독화(Obfuscation)와 최적화가 유기적으로 수행되도록 둡니다.

# 2. 리플렉션 방지 및 난독화 극대화
# 외부 라이브러리 및 SDK 연동 시, API Key가 포함된 서비스 레이어 클래스는 최대한 패키지 구조가 감춰지도록 
# 기본 난독화 옵션을 강하게 적용합니다.
-repackageclasses ''
-allowaccessmodification

# 3. 디버깅 정보 및 소스코드 메타데이터 완전 제거
# 스택 트레이스에서 클래스명과 원본 파일명이 완전히 지워지도록 하여, 디컴파일 시 분석 효율을 극도로 떨어뜨립니다.
-keepattributes !SourceFile,!LineNumberTable

# 4. 중요 설계 제언 (XOR 마스킹 및 JNI 백엔드 프록시 연동)
# ProGuard/R8의 무료 릴리즈는 클래스/메서드 명칭을 암호화하지만, 바이트코드 내부의 "평문 String 상수"는 숨기지 못합니다.
# 상용 배포 단계에서는 다음 중 하나 이상의 보안 레이어를 설계 및 도입하는 것이 철칙입니다:
#   A) XOR / Base64 / AES 등 대칭키 암호화 유틸리티를 적용하여 런타임에 동적으로 키를 디코딩하여 사용하도록 소스코드 설계
#   B) 중요 API Key를 JNI(C/C++ Native Library - .so 파일) 영역으로 내려 정적 분석 및 자바 바이트코드 역컴파일 도구(JADX 등)로부터 보호
#   C) (가장 권장됨) 클라이언트 자체에서 키를 보관하지 않고, 자체 API Gateway 백엔드나 Firebase Functions 등 백엔드 프록시를 거쳐 통신하도록 아키텍처 전환

