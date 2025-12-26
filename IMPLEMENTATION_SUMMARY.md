# SMS Replay - 구현 요약

## 프로젝트 개요
SMS를 수신하여 이메일로 자동 전송하는 Android 애플리케이션

## 구현된 기능

### 1. SMS 수신 및 이메일 전송
- Android BroadcastReceiver로 SMS 수신
- SMTP를 통한 이메일 전송
- TLS/SSL 지원
- 필터링 기능 (발신자 번호, 본문 키워드)

### 2. SMTP 설정
- SMTP 서버 설정 (주소, 포트, ID, 비밀번호)
- 발신자 이메일 자동 생성 (ID@도메인)
- 수신자 이메일 설정
- **연결 테스트 기능**: 저장 시 실제 SMTP 연결 테스트 후 성공 여부 표시
  - 성공: Toast 메시지 + 홈 화면으로 자동 이동
  - 실패: 에러 다이얼로그 표시 (확인/취소 버튼)

### 3. 발송 내역 (히스토리) ✨ NEW
- **데이터베이스**: Room Database v2 (Migration 1→2)
- **저장 항목**:
  - 발신자 번호
  - SMS 본문
  - 수신자 이메일
  - 발신자 이메일
  - 전송 시간
  - 재시도 횟수
- **자동 저장**: 이메일 전송 성공 시 자동으로 히스토리 저장
- **검색 기능**: 발신자 번호 또는 본문 키워드로 검색
- **보관 기간**: 최대 30일 (자동 삭제)
- **개별 삭제**: 각 항목별 삭제 가능
- **UI**: Material Design 3 + 검색창 + LazyColumn

### 4. 필터 설정
- 발신자 번호 필터링
- 본문 키워드 필터링
- DataStore에 영구 저장

### 5. 권한 관리
- SMS 수신/읽기 권한
- 알림 권한 (Android 13+)
- 앱 시작 시 자동 권한 요청
- 권한 거부 시 설정 화면으로 안내

### 6. 서비스 관리
- Foreground Service로 SMS 수신 대기
- 대기열 관리 (네트워크 실패 시 재시도)
- 부팅 시 자동 시작

## 기술 스택

### Architecture
- **Clean Architecture**: Domain, Data, Presentation 계층 분리
- **MVVM**: ViewModel + StateFlow
- **Repository Pattern**: 인터페이스 기반 리포지토리
- **Use Case Pattern**: 단일 책임 UseCase

### UI
- **Jetpack Compose**: 완전한 Compose 기반 UI
- **Material Design 3**: 최신 Material Design
- **Navigation Compose**: 화면 전환

### Data
- **Room Database**: 로컬 데이터베이스
  - `pending_sms`: 대기열 관리
  - `sent_history`: 발송 내역 (v2 추가)
- **DataStore**: 설정 저장 (filter, smtp)
- **EncryptedSharedPreferences**: SMTP 비밀번호 암호화 저장

### DI
- **Koin**: 의존성 주입

### Async
- **Kotlin Coroutines & Flow**: 비동기 처리
- **StateFlow**: UI 상태 관리

### SMTP
- **AndroidJavaMail**: 이메일 전송 라이브러리
- **Jakarta Mail**: SMTP 프로토콜

### Logging
- **Timber**: 로깅 라이브러리

## 주요 파일 구조

```
app/src/main/java/pe/brice/smsreplay/
├── data/
│   ├── dao/
│   │   └── SentHistoryDao.kt          # 히스토리 DAO
│   ├── local/
│   │   ├── database/
│   │   │   └── SmsDatabase.kt         # Room DB v2
│   │   └── entity/
│   │       └── SentHistoryEntity.kt    # 히스토리 Entity
│   ├── repository/
│   │   └── EmailSenderRepositoryImpl.kt # 이메일 전송 + 히스토리 저장
│   └── datastore/
│       └── SecurePreferencesManager.kt  # 암호화된 설정 저장
├── domain/
│   ├── model/
│   │   ├── SentHistory.kt             # 히스토리 도메인 모델
│   │   └── SmtpConfig.kt
│   ├── repository/
│   │   ├── SentHistoryRepository.kt   # 히스토리 리포지토리 인터페이스
│   │   └── EmailSenderRepository.kt
│   └── usecase/
│       ├── GetSentHistoryUseCase.kt
│       ├── AddSentHistoryUseCase.kt
│       ├── DeleteSentHistoryUseCase.kt
│       ├── TestSmtpConnectionUseCase.kt
│       └── SendSmsAsEmailUseCase.kt
├── presentation/
│   ├── di/
│   │   ├── RepositoryModule.kt        # DI 설정
│   │   ├── UseCaseModule.kt
│   │   └── ViewModelModule.kt
│   ├── history/
│   │   ├── SentHistoryScreen.kt       # 히스토리 화면
│   │   └── SentHistoryViewModel.kt
│   ├── smtp/
│   │   ├── SmtpSettingsScreen.kt      # SMTP 설정 화면
│   │   └── SmtpSettingsViewModel.kt
│   └── main/
│       └── MainScreen.kt              # 메인 화면
└── receiver/
    └── SmsReceiver.kt                 # SMS 수신 Receiver
```

## 데이터베이스 스키마

### sent_history 테이블 (v2 추가)
```sql
CREATE TABLE sent_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sender TEXT NOT NULL,              -- 발신자 번호
    body TEXT NOT NULL,                -- SMS 본문
    timestamp INTEGER NOT NULL,         -- SMS 수신 시간
    recipientEmail TEXT NOT NULL,      -- 수신자 이메일
    senderEmail TEXT NOT NULL,         -- 발신자 이메일
    sentAt INTEGER NOT NULL,           -- 이메일 전송 시간
    retryCount INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX index_sent_history_timestamp ON sent_history(timestamp);
CREATE INDEX index_sent_history_sender ON sent_history(sender);
CREATE INDEX index_sent_history_body ON sent_history(body);
```

## 주요 기능 구현 상세

### 1. 이메일 전송 후 히스토리 자동 저장
**파일**: `EmailSenderRepositoryImpl.kt`

```kotlin
result.fold(
    onSuccess = {
        // 이메일 전송 성공 - 히스토리 저장
        val history = SentHistory(
            sender = smtpConfig.username,
            body = email.subject,
            timestamp = email.timestamp,
            recipientEmail = smtpConfig.recipientEmail,
            senderEmail = smtpConfig.senderEmail,
            sentAt = System.currentTimeMillis(),
            retryCount = 0
        )
        addSentHistoryUseCase(history)
        SendingResult.Success
    },
    onFailure = { /* 에러 처리 */ }
)
```

### 2. SMTP 연결 테스트
**파일**: `SmtpSettingsViewModel.kt`

```kotlin
fun saveSettings() {
    // 1. 저장
    saveSmtpConfigUseCase(config)

    // 2. 연결 테스트
    val testResult = testSmtpConnectionUseCase()
    testResult.fold(
        onSuccess = {
            // 연결 성공 → 상태 업데이트 → 화면 이동
            _uiState.value = state.copy(isSuccess = true)
        },
        onFailure = { exception ->
            // 연결 실패 → 에러 메시지 표시
            _uiState.value = state.copy(
                errorMessage = "SMTP 연결 실패: ${exception.message}"
            )
        }
    )
}
```

### 3. 히스토리 검색
**파일**: `SentHistoryDao.kt`

```kotlin
@Query("""
    SELECT * FROM sent_history
    WHERE sender LIKE '%' || :keyword || '%'
    OR body LIKE '%' || :keyword || '%'
    ORDER BY sentAt DESC
""")
fun searchHistory(keyword: String): Flow<List<SentHistoryEntity>>
```

### 4. 30일 자동 삭제
**파일**: `SentHistoryRepositoryImpl.kt`

```kotlin
override suspend fun addHistory(history: SentHistory): Long {
    deleteOldRecords() // 30일 지난 레코드 삭제
    val entity = history.toEntity()
    return sentHistoryDao.insert(entity)
}

private suspend fun deleteOldRecords() {
    val cutoffTime = System.currentTimeMillis() -
                     TimeUnit.DAYS.toMillis(SentHistory.MAX_HISTORY_DAYS)
    sentHistoryDao.deleteOldRecords(cutoffTime)
}
```

## UI/UX 개선사항

### 1. 권한 요청
- 앱 시작 시 즉시 권한 요청
- 권한 카드가 허용 상태에 따라 자동 표시/숨김

### 2. SMTP 설정
- 발신자 이메일 자동 생성 (입력 필드 제거)
- 연결 테스트 중 로딩 표시
- 에러 다이얼로그 (확인/취소 모두 지원)

### 3. 발송 내역
- 검색창 (상단 고정)
- 빈 상태 표시
- 타임스탬프 포맷팅 ("방금 전", "5분 전", "어제", etc.)
- 스와이프 삭제 기능

## 빌드 정보
- **compileSdk**: 35
- **minSdk**: 26
- **targetSdk**: 35
- **Kotlin**: 2.1.0
- **Compose BOM**: 2024.12.01
- **Room**: 2.6.1

## 마이그레이션
- **v1 → v2**: `sent_history` 테이블 추가
  - 기존 `pending_sms` 데이터 보존
  - 인덱스 추가로 검색 성능 최적화

## 테스트 필드 사항
- [ ] 다양한 SMTP 서버 테스트 (Gmail, Naver, Daum)
- [ ] 대용량 SMS 수신 시 테스트
- [ ] 네트워크 실패 후 재시도 테스트
- [ ] 히스토리 자동 삭제 테스트 (30일 후)
- [ ] 권한 거부 시 동작 테스트

## 다음 단계 (옵션)
1. **통계 기능**: 발송 성공률, 일별 발송량 등
2. **내보내기**: CSV/엑셀로 히스토리 내보내기
3. **차단 기능**: 특정 발신자/키워드 차단
4. **알림**: SMS 수신/전송 알림 표시
5. **다중 수신자**: 여러 이메일 주소로 전송
