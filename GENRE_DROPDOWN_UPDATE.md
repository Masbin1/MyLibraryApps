# Update Genre Field di EditBookFragment

## Perubahan yang Dilakukan

Mengubah field Genre di EditBookFragment dari TextInputEditText menjadi AutoCompleteTextView (dropdown selection) seperti di AddBookFragment.

### 1. Perubahan Layout (fragment_edit_book.xml)

**Sebelum:**
```xml
<com.google.android.material.textfield.TextInputEditText
    android:id="@+id/etGenre"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint=""
    android:textColor="#000000"
    android:textColorHint="#888888" />
```

**Sesudah:**
```xml
<AutoCompleteTextView
    android:id="@+id/actvGenre"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:clickable="true"
    android:focusable="false"
    android:inputType="none"
    android:textColor="#000000" />
```

### 2. Perubahan Kode (EditBookFragment.kt)

#### Import yang Ditambahkan:
```kotlin
import android.widget.ArrayAdapter
```

#### Method Baru:
```kotlin
private fun setupGenreDropdown() {
    val genres = resources.getStringArray(R.array.book_types)
    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genres)
    binding.actvGenre.setAdapter(adapter)
    binding.actvGenre.setOnClickListener { binding.actvGenre.showDropDown() }
}
```

#### Perubahan di onViewCreated():
```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    book = arguments?.getParcelable("book") ?: return
    setupGenreDropdown() // ← Ditambahkan
    populateFormWithBookData()
    // ... rest of the code
}
```

#### Perubahan di populateFormWithBookData():
```kotlin
// Sebelum:
binding.etGenre.setText(book.genre)

// Sesudah:
binding.actvGenre.setText(book.genre, false)
```

#### Perubahan di updateBook():
```kotlin
// Sebelum:
genre = binding.etGenre.text.toString()

// Sesudah:
genre = binding.actvGenre.text.toString()
```

### 3. Genre Options yang Tersedia

Genre options diambil dari `arrays.xml`:
```xml
<string-array name="book_types">
    <item>Sastra</item>
    <item>Sejarah</item>
    <item>Agama</item>
    <item>Sains</item>
    <item>Cerita</item>
    <item>Budaya</item>
</string-array>
```

## Hasil

Sekarang field Genre di EditBookFragment:
- ✅ Menggunakan dropdown selection seperti di AddBookFragment
- ✅ Menampilkan pilihan genre yang sama
- ✅ Dapat menampilkan genre yang sudah ada saat edit
- ✅ User dapat memilih genre baru dari dropdown
- ✅ Konsisten dengan UI/UX di AddBookFragment

## Testing

Untuk menguji perubahan:
1. Buka halaman Edit Book
2. Klik field Genre
3. Dropdown akan muncul dengan pilihan genre
4. Pilih genre baru
5. Simpan perubahan
6. Verifikasi genre tersimpan dengan benar