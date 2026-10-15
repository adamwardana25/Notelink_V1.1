# Implementation Plan - Task 2: Dashboard and CRUD Operations

Implement the Dashboard UI and CRUD logic for bookmarks in the Notelink app.

## Proposed Changes

### Data Layer

#### [NEW] [Bookmark.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/data/model/Bookmark.kt)
Define the `Bookmark` data model with fields for id, title, url, category, isFavorite, and timestamp.

#### [NEW] [BookmarkDao.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/data/local/BookmarkDao.kt)
Room DAO for bookmark operations (Insert, Update, Delete, Query).

#### [NEW] [AppDatabase.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/data/local/AppDatabase.kt)
Room database configuration.

#### [NEW] [BookmarkRepository.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/data/repository/BookmarkRepository.kt)
Repository to abstract data access.

### UI Layer

#### [MODIFY] [HomeScreen.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/ui/home/HomeScreen.kt)
Implement the Dashboard UI:
- Profile header.
- Search bar and category filters.
- Bookmark list with card layout.
- FAB for adding new bookmarks.
- Empty state view.

#### [NEW] [HomeViewModel.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/ui/home/HomeViewModel.kt)
ViewModel to manage UI state, search/filter logic, and CRUD operations.

#### [NEW] [BookmarkComponents.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/ui/home/components/BookmarkComponents.kt)
Reusable UI components like `BookmarkCard`, `CategoryChips`, `AddEditBookmarkDialog`, etc.

### Navigation

#### [MODIFY] [MainActivity.kt](file:///D:/AndroidNative/Notelink/app/src/main/java/com/example/notelink/MainActivity.kt)
Ensure `HomeViewModel` is provided to `HomeScreen`.

## Verification Plan

### Automated Tests
- Unit tests for `HomeViewModel` filtering and sorting logic.
- Room database migration/integrity tests (if applicable).

### Manual Verification
- Verify Add, Edit, Delete operations.
- Verify Search and Filter functionality.
- Verify Empty State display.
- Verify theme and responsiveness.
