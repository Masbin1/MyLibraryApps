# Sticky Recommendations Implementation

## Problem
Fitur collaborative filtering untuk rekomendasi buku menimpa list buku di home screen. Ketika user scroll ke bawah untuk melihat lebih banyak buku, rekomendasi ikut menghilang dan user harus scroll kembali ke atas untuk melihat rekomendasi.

## Solution
Implementasi **Sticky Recommendations** menggunakan `CoordinatorLayout` dan `AppBarLayout` dengan `layout_scrollFlags="noScroll"` untuk membuat section rekomendasi tetap terlihat saat user scroll.

## Changes Made

### 1. Layout Structure Changes (`fragment_home.xml`)
- **Before**: `FrameLayout` → `LinearLayout` → `ScrollView`
- **After**: `CoordinatorLayout` → `AppBarLayout` + `NestedScrollView`

### 2. Key Components

#### AppBarLayout Structure:
1. **Header** (`scroll|enterAlways`) - Header akan scroll dan kembali muncul saat scroll up
2. **Search & Filter** (`scroll|enterAlways`) - Ikut scroll dengan header
3. **Recommendations** (`noScroll`) - **STICKY** - Tidak akan scroll, selalu terlihat

#### Scrollable Content:
- `NestedScrollView` dengan `app:layout_behavior="@string/appbar_scrolling_view_behavior"`
- Contains book list RecyclerView

### 3. RecyclerView Adjustments (`HomeFragment.kt`)
- Added `isNestedScrollingEnabled = false` untuk kedua RecyclerView
- Recommendations RecyclerView tetap horizontal
- Books RecyclerView dalam NestedScrollView

### 4. Visual Enhancements
- Added `android:elevation="4dp"` pada recommendations section
- Background color consistency

## How It Works

1. **Initial State**: User melihat header, search, dan rekomendasi
2. **Scroll Down**: 
   - Header dan search bar menghilang ke atas
   - **Rekomendasi tetap terlihat** (sticky)
   - List buku scroll normal
3. **Scroll Up**: Header dan search bar kembali muncul

## Benefits

✅ **Rekomendasi selalu accessible** - User tidak perlu scroll ke atas lagi
✅ **Better UX** - Dual layout yang tidak saling menimpa
✅ **Smooth scrolling** - Natural Android scrolling behavior
✅ **Space efficient** - Header bisa disembunyikan untuk lebih banyak ruang konten

## Technical Details

- Uses Material Design's `CoordinatorLayout` behavior
- Compatible dengan existing collaborative filtering logic
- No breaking changes pada ViewModel atau data layer
- Maintains all existing functionality (search, filter, notifications)

## Testing
- Build successful ✅
- No compilation errors ✅
- Maintains backward compatibility ✅