# Task 1: Foundation and Authentication

This task involves setting up the project infrastructure (Room, Firebase, Navigation 3, Adaptive UI) and implementing Google Sign-In authentication.

## User Review Required

> [!IMPORTANT]
> I need the `google-services.json` file from your Firebase project to complete the Firebase setup. Please provide it or let me know if you want me to skip the actual connection for now.

> [!NOTE]
> I could not find the provided screenshots (`input_file_0.png`, `input_file_1.png`, `input_file_2.png`) in the project directory. I will proceed with a clean Material 3 design using dynamic color unless you can point me to where these files are or provide descriptions.

## Proposed Changes

### [Component] Dependency Management

#### [MODIFY] [libs.versions.toml](file:///D:/AndroidNative/Notelink/gradle/libs.versions.toml)
Add Firebase, Google Play Services Auth, and Google Services plugin versions and libraries.

#### [MODIFY] [build.gradle.kts](file:///D:/AndroidNative/Notelink/build.gradle.kts) (root)
Add Google Services plugin to the plugins block.

#### [MODIFY] [build.gradle.kts](file:///D:/AndroidNative/Notelink/app/build.gradle.kts)
Apply Google Services plugin and add dependencies for Firebase and Auth.

---

### [Component] Room Database

#### [NEW] [Link.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/data/local/Link.kt)
Define the `Link` entity.

#### [NEW] [LinkDao.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/data/local/LinkDao.kt)
Define the DAO for Link entity.

#### [NEW] [AppDatabase.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/data/local/AppDatabase.kt)
Define the Room database class.

---

### [Component] Authentication

#### [NEW] [AuthRepository.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/data/repository/AuthRepository.kt)
Interface and implementation for Firebase Google Sign-In.

#### [NEW] [AuthViewModel.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/ui/auth/AuthViewModel.kt)
ViewModel to manage authentication state.

---

### [Component] UI & Navigation

#### [NEW] [LoginScreen.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/ui/auth/LoginScreen.kt)
Implement the Login screen with Google Sign-In button.

#### [MODIFY] [MainActivity.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/MainActivity.kt)
Set up Navigation 3 and entry point for the app.

---

## Verification Plan

### Automated Tests
- Room DB: Unit test for LinkDao (Insert, Query).
- Navigation: Verify initial route is Login.

### Manual Verification
- Build the app and verify the Login screen appears.
- (Optional, if `google-services.json` provided) Verify Google Sign-In flow.
