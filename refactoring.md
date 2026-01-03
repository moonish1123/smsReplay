# Clean Architecture Refactoring Plan

## Overview

This document outlines Clean Architecture violations found in the SMS Replay Android app and provides a structured refactoring roadmap.

**Analysis Date**: 2025-12-28
**Project**: SMS Replay Android App
**Architecture**: Clean Architecture with Domain/Data/Presentation layers

---

## Summary of Findings

| Priority | Count | Key Issues |
|----------|-------|------------|
| 🔴 HIGH | 2 | Domain layer exposing Data entities, Android dependency in Domain |
| 🟡 MEDIUM | 4 | Layer boundary violations, business logic in Presentation |
| 🟢 LOW | 3 | Thin use cases, service abstraction opportunities |

---

## Phase 1: Critical Fixes (1-2 days)

### 🔴 HIGH-001: Remove Android Framework Dependency from Domain Model

**Location**: `app/src/main/java/pe/brice/smsreplay/domain/model/SmtpConfig.kt:21-22`

**Current Issue**:
```kotlin
import android.util.Patterns.EMAIL_ADDRESS

data class SmtpConfig(...) {
    fun isValid(): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(senderEmail).matches() &&
               areRecipientEmailsValid()
    }
}
```

**Problem**: Domain Model depends on Android framework - violates platform independence principle.

**Refactoring Steps**:
1. Create `pe.brice.smsreplay.domain.validation.EmailValidator` object
2. Implement pure Kotlin email regex validation
3. Update `SmtpConfig.isValid()` to use `EmailValidator`
4. Add unit tests for email validation

**Files to Modify**:
- Create: `domain/validation/EmailValidator.kt`
- Modify: `domain/model/SmtpConfig.kt`
- Create: `domain/validation/EmailValidatorTest.kt`

**Validation**: Unit tests pass without Android dependencies

---

### 🔴 HIGH-002: Domain Repository Exposing Data Layer Entities

**Location**: `app/src/main/java/pe/brice/smsreplay/domain/repository/SmsQueueRepository.kt`

**Current Issue**:
```kotlin
interface SmsQueueRepository {
    fun getAllPendingSms(): Flow<List<PendingSmsEntity>>  // Data Layer entity!
    suspend fun getNextPendingSms(): PendingSmsEntity?     // Data Layer entity!
    suspend fun findByTimestamp(timestamp: Long): PendingSmsEntity?
}
```

**Problem**: Domain Layer interface exposes Data Layer entities - violates Dependency Rule.

**Refactoring Steps**:
1. Change return types to Domain model (`SmsMessage`)
2. Create `internal interface PendingSmsDao` for Data Layer operations
3. Update `SmsQueueRepositoryImpl` to implement both interfaces
4. Update `SmsQueueManager` to use Domain models or internal DAO

**Files to Modify**:
- Modify: `domain/repository/SmsQueueRepository.kt`
- Create: `data/local/dao/PendingSmsDao.kt` (internal)
- Modify: `data/repository/SmsQueueRepositoryImpl.kt`
- Modify: `work/SmsQueueManager.kt`

**Validation**: Domain layer has no imports from `data.*` packages

---

## Phase 2: Layer Boundary Cleanup (2-3 days)

### 🟡 MED-001: Move DI Module to Correct Package

**Location**: `app/src/main/java/pe/brice/smsreplay/presentation/di/RepositoryModule.kt`

**Current Issue**:
```kotlin
// File is in presentation/di but imports Data Layer classes
import pe.brice.smsreplay.data.datastore.FilterSettingsDataStore
import pe.brice.smsreplay.data.dao.SentHistoryDao
```

**Problem**: DI module in wrong package creates layer boundary confusion.

**Refactoring Steps**:
1. Create `app/src/main/java/pe/brice/smsreplay/di/` package
2. Move `RepositoryModule.kt` to new location
3. Create separate `PresentationModule.kt` for ViewModels
4. Update imports across project

**Files to Modify**:
- Move: `presentation/di/RepositoryModule.kt` → `di/RepositoryModule.kt`
- Create: `presentation/di/PresentationModule.kt`
- Update: `SmsReplayApplication.kt`

**Validation**: Each package only contains its own layer's dependencies

---

### 🟡 MED-002: Infrastructure Layer Using Data Entities

**Location**: `app/src/main/java/pe/brice/smsreplay/work/SmsQueueManager.kt:152`

**Current Issue**:
```kotlin
fun getAllPending(): Flow<List<PendingSmsEntity>> {
    return smsQueueRepository.getAllPendingSms()
}
```

**Problem**: Infrastructure/Work layer bypasses Domain Layer abstraction.

**Refactoring Steps**:
1. Refactor `SmsQueueManager` to use `SmsMessage` Domain model
2. For entity-level operations, use internal `PendingSmsDao`
3. Add mapping functions in repository implementation
4. Update WorkManager integration if needed

**Files to Modify**:
- Modify: `work/SmsQueueManager.kt`
- Modify: `data/repository/SmsQueueRepositoryImpl.kt`
- Modify: `data/local/dao/PendingSmsDao.kt` (if created)

**Validation**: `SmsQueueManager` has no imports from `data.local.database`

---

### 🟡 MED-003: Use Case Async Fire-and-Forget Pattern

**Location**: `app/src/main/java/pe/brice/smsreplay/domain/usecase/SendSmsAsEmailUseCase.kt:80-97`

**Current Issue**:
```kotlin
if (result is SendingResult.Success) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val history = SentHistory(...)
            addSentHistoryUseCase(history)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save sent history")
        }
    }
}
```

**Problem**: Fire-and-forget async operation with silent failure - improper error handling.

**Refactoring Steps**:
1. Make history saving synchronous part of use case
2. Remove fire-and-forget pattern
3. Return complete result including history save status
4. Update caller to handle errors properly

**Files to Modify**:
- Modify: `domain/usecase/SendSmsAsEmailUseCase.kt`
- Modify: `service/SmsForegroundService.kt` (caller)

**Validation**: All operations complete before returning result

---

### 🟡 MED-004: ViewModel Contains Business Logic

**Location**: `app/src/main/java/pe/brice/smsreplay/presentation/smtp/SmtpSettingsViewModel.kt:74-98`

**Current Issue**:
```kotlin
fun getSenderEmail(): String {
    // Business logic: Extract domain from server address
    val domain = state.serverAddress
        .removePrefix("smtp.")
        .removePrefix("mail.")
    return "${state.username}@$domain"
}
```

**Problem**: Email generation logic belongs in Domain Layer, not ViewModel.

**Refactoring Steps**:
1. Create `GenerateSenderEmailUseCase`
2. Create `ValidateSmtpConfigUseCase`
3. Move email generation logic to use case
4. Update ViewModel to use new use cases

**Files to Modify**:
- Create: `domain/usecase/GenerateSenderEmailUseCase.kt`
- Create: `domain/usecase/ValidateSmtpConfigUseCase.kt`
- Modify: `presentation/smtp/SmtpSettingsViewModel.kt`
- Modify: `presentation/di/PresentationModule.kt`

**Validation**: ViewModel only handles UI state, no business logic

---

## Phase 3: Responsibility Alignment (3-4 days)

### 🟢 LOW-001: Thin Pass-Through Use Cases

**Locations**:
- `domain/usecase/SaveSmtpConfigUseCase.kt`
- `domain/usecase/TestSmtpConnectionUseCase.kt`
- `domain/usecase/GetSmtpConfigUseCase.kt`

**Current Issue**:
```kotlin
class SaveSmtpConfigUseCase(
    private val smtpConfigRepository: SmtpConfigRepository
) {
    suspend operator fun invoke(config: SmtpConfig) {
        smtpConfigRepository.saveSmtpConfig(config) // Just pass-through
    }
}
```

**Problem**: Use Cases with no business logic create unnecessary abstraction.

**Refactoring Steps**:
1. Audit all use cases for business logic value
2. Remove thin use cases, use repository directly in ViewModel
3. OR consolidate related operations into cohesive use cases
4. Update DI configuration

**Files to Modify**:
- Delete/Modify: Multiple use case files
- Modify: ViewModels using these use cases
- Modify: DI modules

**Validation**: Each use case contains meaningful business logic

---

### 🟢 LOW-002: ViewModel Interprets Domain Model State

**Location**: `app/src/main/java/pe/brice/smsreplay/presentation/main/MainViewModel.kt:94`

**Current Issue**:
```kotlin
val isConfigured = config?.isValid() ?: false // ViewModel interprets domain state
```

**Problem**: ViewModel should receive ready-to-use UI state from Domain Layer.

**Refactoring Steps**:
1. Create `GetSmtpConfigurationStatusUseCase`
2. Return `SmtpConfigurationStatus` data class
3. ViewModel only applies to UI state
4. Remove validation logic from ViewModel

**Files to Modify**:
- Create: `domain/usecase/GetSmtpConfigurationStatusUseCase.kt`
- Create: `domain/model/SmtpConfigurationStatus.kt`
- Modify: `presentation/main/MainViewModel.kt`

**Validation**: ViewModel doesn't call `isValid()` or interpret domain state

---

### 🟢 LOW-003: Abstract Infrastructure Services

**Location**: `app/src/main/java/pe/brice/smsreplay/presentation/main/MainViewModel.kt:15-18`

**Current Issue**:
```kotlin
private val serviceManager: ServiceManager by inject()
private val permissionManager: PermissionManager by inject()
private val batteryOptimizationManager: BatteryOptimizationManager by inject()
```

**Problem**: Direct dependency on concrete infrastructure services.

**Refactoring Steps**:
1. Create domain interfaces for services with business logic
2. Implement interfaces in service layer
3. Update ViewModel to depend on interfaces
4. Update DI configuration

**Files to Modify**:
- Create: `domain/service/ServiceControl.kt` (interface)
- Create: `domain/service/PermissionChecker.kt` (interface)
- Modify: `service/ServiceManager.kt` (implement interface)
- Modify: `service/PermissionManager.kt` (implement interface)
- Modify: `presentation/main/MainViewModel.kt`

**Validation**: ViewModel depends on interfaces from Domain Layer

---

## Execution Order

### Sprint 1 (Week 1)
1. 🔴 HIGH-001: Remove Android dependency from SmtpConfig
2. 🔴 HIGH-002: Fix Domain Repository entity exposure
3. 🟡 MED-003: Fix async fire-and-forget pattern

### Sprint 2 (Week 2)
4. 🟡 MED-001: Move DI module to correct package
5. 🟡 MED-002: Infrastructure layer domain model usage
6. 🟡 MED-004: Extract business logic from ViewModel

### Sprint 3 (Week 3)
7. 🟢 LOW-001: Remove thin use cases
8. 🟢 LOW-002: Create status use cases
9. 🟢 LOW-003: Abstract infrastructure services

---

## Testing Strategy

### Before Refactoring
1. Create baseline test suite
2. Document current behavior
3. Identify critical user flows

### During Refactoring
1. Write tests for new components first (TDD)
2. Verify layer isolation (no cross-layer imports)
3. Run integration tests after each phase

### After Refactoring
1. Measure test coverage per layer
2. Verify Android dependency free Domain Layer
3. Performance regression testing

---

## Success Criteria

✅ **Layer Isolation**: No layer imports from layers it shouldn't
✅ **Domain Independence**: Domain layer has zero Android dependencies
✅ **Single Responsibility**: Each class has one clear purpose
✅ **Testability**: All layers can be unit tested in isolation
✅ **Maintainability**: New features don't require cross-layer changes

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking existing functionality | HIGH | Comprehensive test suite, incremental changes |
| Increased complexity | MEDIUM | Clear documentation, code reviews |
| Performance regression | LOW | Profile before/after, optimize bottlenecks |
| Time overrun | MEDIUM | Prioritize by value, can defer LOW items |

---

## Notes

- All refactoring should be done incrementally with tests
- Each phase should be committed separately for easy rollback
- Consider feature flags for critical paths if needed
- Update documentation after each phase

---

**Last Updated**: 2025-12-28
**Next Review**: After Sprint 1 completion
