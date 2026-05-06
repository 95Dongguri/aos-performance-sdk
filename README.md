## Why Performance SDK?

기존 Firebase Performance Monitoring은 다음과 같은 한계가 있다고 판단했습니다.

- 프레임 단위의 세밀한 jank 분석이 어려움
- WebView 렌더링 성능 측정 지원 부족
- 커스터마이징 및 경량화 한계

이러한 문제를 해결하기 위해, 
네이티브 + WebView 성능을 통합적으로 측정할 수 있는 경량 SDK를 개발했습니다.

## Key Features

- 📱 디바이스 refresh rate 기반 프레임 budget 계산
- 🎯 최근 60프레임 기준 jankRate 계산 (실시간 체감 성능 반영)
- 🌐 WebView requestAnimationFrame 기반 렌더링 측정
- 🧩 Native + WebView 통합 성능 분석
- ⚡ 경량 SDK (Firebase 대비 낮은 오버헤드)

## Recommended Usage

- 디버그 빌드에서 DebugOverlay를 활용해 실시간 성능 모니터링
- 특정 화면 (예: 리스트, WebView 화면)에서만 FrameTracker 활성화
- 릴리스 환경에서는 샘플링 기반 수집 권장

## 샘플 앱·영상

리포지토리의 `app` 모듈에 **Application 초기화**, **FrameTracker**, **WebView 연동**, **무거운 리스트(jank 유도)** 예제가 있습니다.

[시현 영상](./demo.gif)

## 제한 사항·주의

- WebView `JavascriptInterface`는 **신뢰할 수 있는 콘텐츠**에 사용하는 것이 안전합니다.
- 오버레이는 **디버그/개발 목적**에 가깝습니다. 릴리스 빌드에서는 표시 여부를 앱 정책에 맞게 제어하세요.

## 요구 사항

| 항목 | 값 |
|------|-----|
| **minSdk** | **24** |
| **compileSdk** (이 모듈 기준) | **36** |
| **Java** | **17** (`sourceCompatibility` / `targetCompatibility`) |
| **Android Gradle Plugin** | 프로젝트 기준 **9.2.0** (AGP 9는 **내장 Kotlin** 사용, `org.jetbrains.kotlin.android` 플러그인 불필요) |
| **Gradle** | AGP 9.2 호환 버전 (예: **9.4.1**) |

앱 모듈은 일반적으로 `compileSdk` / `targetSdk`를 **36 이상**으로 맞추는 것을 권장합니다.

## 모듈 의존성

SDK는 다음 AndroidX 라이브러리에 의존합니다.

- `androidx.core:core-ktx`
- `androidx.lifecycle:lifecycle-runtime-ktx`

## Gradle에서 사용하기

### 멀티모듈(같은 리포지토리)

`app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":performance-sdk"))
}
```

## Get Started

### 1) 초기화 (`Application`)

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PerformanceMonitor.init(
            this,
            PerformanceConfig(debugLogging = BuildConfig.DEBUG),
        )
    }
}
```

`AndroidManifest.xml`에 `android:name`으로 Application 클래스를 등록합니다.

### 2) 모니터링 시작/종료

```kotlin
PerformanceMonitor.start()
// …
PerformanceMonitor.stop()  // WebView 브리지도 함께 정리됨
```

### 3) 네이티브 프레임 수집 (`FrameTracker`)

`Choreographer` 기반으로 **이전 vsync 대비 프레임 간격(ms)** 을 콜백으로 받습니다. 수집값은 `PerformanceMonitor.recordFrameTimeMs`로 넘깁니다.

```kotlin
val tracker = FrameTracker().apply {
    bindTo(lifecycle)  // Activity/Fragment lifecycle 권장
    start { ms -> PerformanceMonitor.recordFrameTimeMs(ms) }
}
// tracker.stop()  // 필요 시; lifecycle 바인딩 시 destroy에서 정리
```

- `start()`는 **메인 스레드**에서 호출해야 합니다.
- `stop()`는 어느 스레드에서도 호출 가능합니다.

### 4) WebView 수집

페이지 로드 후(문서 컨텍스트가 있을 때) **메인 스레드**에서 연결합니다.

```kotlin
PerformanceMonitor.attachWebView(webView) { data ->
    // 선택: WebPerformanceData(renderTimeMs) 추가 처리
}
```

종료 시:

```kotlin
PerformanceMonitor.detachWebView(webView)
```

WebView에서 JS가 동작해야 하므로 `WebSettings.javaScriptEnabled = true`가 필요합니다. 샘플은 `WebViewClient.onPageFinished` 이후에 붙입니다.

### 5) 집계 결과 읽기

```kotlin
val m = PerformanceMonitor.aggregator.getMetrics()
// m.fps, m.averageFrameTimeMs, m.jankCount, m.jankRate, m.averageWebRenderTimeMs, m.state
```

또는 직접:

```kotlin
val aggregator = PerformanceAggregator()
// recordFrameTimeMs / recordWebRenderTimeMs …
aggregator.getMetrics()
```

## DebugOverlay

액티비티 **윈도우 decor**에 작은 텍스트 오버레이를 붙입니다. **`SYSTEM_ALERT_WINDOW` 권한은 필요 없습니다.**

```kotlin
val overlay = DebugOverlay(activity) { PerformanceMonitor.aggregator.getMetrics() }
overlay.show()
// …
overlay.hide()
```

- 기본 갱신 주기: **500ms**
- 상태 색: `PerformanceState`에 따라 배지 배경색 변경
- 드래그로 위치 이동 가능 (상태바 아래로 클램프)

## 메트릭·상태 정의

### `jankRate` (최근 윈도우)

- **최근 최대 60프레임**에 대해 `jankRate = (jank 프레임 수) / (해당 윈도우 프레임 수)` 입니다.
- 샘플이 60개 미만이면 현재까지 쌓인 개수로 나눕니다.

### `Jank` 판정 (네이티브 프레임)

- `PerformanceAggregator` 내부에서 **`frameTimeMs > JANK_THRESHOLD_MS`** 이면 jank로 카운트합니다. (현재 임계값은 소스의 상수 참고)
- 60Hz에서 정상 간격이 약 **16.67ms**인 경우, 임계값이 **16ms**이면 정상 프레임도 jank로 잡힐 수 있습니다. 운영 기준에 맞게 상수 조정을 검토하세요.
  * `현 기준 임계값 17로 설정하였습니다.`

### `PerformanceState`

`fps`와 위 `jankRate`로 대략 다음 규칙을 적용합니다 (소스 기준).

- `fps >= 55 && jankRate < 0.05` → `GOOD`
- `fps >= 45 && jankRate < 0.15` → `WARNING`
- 그 외 → `BAD`

## R8 / ProGuard

라이브러리는 `consumer-rules.pro`로 **`JsBridge`의 `@JavascriptInterface`** 가 난독화로 제거되지 않도록 규칙을 포함합니다. 앱에서 R8을 켠 경우에도 소비자 쪽에 전달됩니다.
