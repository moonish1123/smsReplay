# SMS Replay 📤

**SMS Replay**는 수신된 SMS 메시지를 사용자가 설정한 이메일로 자동으로 전달해주는 안드로이드 애플리케이션입니다. 여러 대의 단말기를 사용하는 사용자가 하나의 이메일 계정에서 모든 문자를 통합 관리할 수 있도록 도와줍니다.

## ✨ 주요 기능 (Key Features)

*   **✉️ SMS 자동 전달**: 문자가 수신되면 설정된 SMTP 서버를 통해 즉시 이메일로 전송합니다.
*   **🔍 스마트 필터링**:
    *   **발신자 번호**: 특정 번호에서 온 문자만 전송할 수 있습니다.
    *   **본문 키워드**: 쉼표(`,`)로 구분된 여러 키워드 중 하나라도 포함되면 전송합니다. (예: `결제, 인증, 택배` → OR 조건)
*   **📱 멀티 디바이스 지원**: 각 단말기마다 **별칭(Alias)**을 설정하여, 이메일 본문과 발신자 이름에서 어떤 폰으로 온 문자인지 쉽게 식별할 수 있습니다.
    *   메일 제목: `[FW SMS] 010-1234-5678 (내 업무폰)`
*   **🎨 직관적인 이메일 템플릿**: 카카오톡 스타일의 깔끔한 카드 뷰 디자인으로 가독성을 높였습니다. (다크 모드 지원)
*   **🛡️ 안정적인 동작**:
    *   **오프라인 대기열**: 네트워크가 없을 때 받은 문자는 내부 DB에 저장했다가 연결 시 자동 전송합니다.
    *   **Foreground Service**: 앱이 종료되어도 백그라운드에서 안정적으로 문자를 수신합니다.
    *   **자동 재시도**: 전송 실패 시 WorkManager를 통해 지수 백오프(Exponential Backoff) 방식으로 재시도합니다.

## 🛠️ 기술 스택 (Tech Stack)

*   **Language**: Kotlin
*   **Architecture**: Clean Architecture (Data, Domain, Presentation, Infrastructure Layers)
*   **UI**: Jetpack Compose (Material Design 3)
*   **DI**: Koin 4.0
*   **Async**: Coroutines & Flow
*   **Database**: Room
*   **Background**: WorkManager, Foreground Service
*   **Security**: EncryptedSharedPreferences (Jetpack Security)
*   **Networking**: AndroidJavaMail (SMTP)

## 🚀 시작하기 (Getting Started)

### 1. 권한 설정
앱을 처음 실행하면 다음 권한을 요청합니다. 원활한 동작을 위해 모두 허용해주세요.
*   **SMS 수신/읽기**: 문자 내용을 가져오기 위해 필수입니다.
*   **알림 (Android 13+)**: 서비스 상태를 알리기 위해 필요합니다.
*   **배터리 최적화 제외**: 백그라운드에서 서비스가 죽지 않도록 필수적으로 설정해야 합니다.

### 2. SMTP 설정
이메일을 보내기 위해 사용하는 메일 서버 정보를 입력합니다. (예: Gmail, Naver, Daum)
*   **서버 주소**: `smtp.gmail.com` (포트 587)
*   **아이디/비밀번호**: 메일 계정 정보 (2단계 인증 사용 시 **앱 비밀번호** 필요)
*   **단말기 별칭**: 이 폰을 구분할 이름 (예: `메인폰`, `업무폰`)

### 3. 필터 설정 (선택)
모든 문자를 받고 싶다면 설정하지 않아도 됩니다.
*   스팸 문자를 거르고 중요 알림만 받고 싶을 때 유용합니다.

## 🔒 보안 및 개인정보

*   사용자의 SMTP 비밀번호는 안드로이드의 **Keystore**를 이용해 암호화되어 안전하게 저장됩니다.
*   모든 데이터는 사용자 기기 내부에만 저장되며, 설정한 이메일 주소 외의 외부 서버로 전송되지 않습니다.

## 🤝 기여 (Contributing)

이 프로젝트는 오픈 소스입니다. 버그 제보나 기능 제안은 Issue를 통해 남겨주세요.

## 📄 라이선스

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
