# MyLibraryApps - Class Diagram

## File yang Dibuat

1. **MyLibraryApps_ClassDiagram.puml** - File PlantUML untuk class diagram
2. **CLASS_DIAGRAM_README.md** - File ini (instruksi penggunaan)

## Cara Generate Gambar dari File PlantUML

### Opsi 1: Online PlantUML Editor (Paling Mudah)
1. Buka website: https://www.plantuml.com/plantuml/uml/
2. Copy seluruh isi file `MyLibraryApps_ClassDiagram.puml`
3. Paste ke editor online
4. Klik "Submit" untuk generate gambar
5. Download gambar dalam format PNG/SVG

### Opsi 2: VS Code Extension
1. Install extension "PlantUML" di VS Code
2. Buka file `MyLibraryApps_ClassDiagram.puml`
3. Tekan `Alt + D` atau `Ctrl + Shift + P` → "PlantUML: Preview Current Diagram"
4. Export ke PNG/SVG dari preview

### Opsi 3: Command Line (Perlu Java)
1. Download PlantUML JAR: http://plantuml.com/download
2. Jalankan command:
   ```bash
   java -jar plantuml.jar MyLibraryApps_ClassDiagram.puml
   ```
3. File PNG akan ter-generate otomatis

### Opsi 4: IntelliJ IDEA / Android Studio Plugin
1. Install plugin "PlantUML integration"
2. Buka file `.puml`
3. Klik icon "Show diagram" atau tekan `Ctrl + Shift + F12`
4. Export dari diagram viewer

## Struktur Class Diagram

### Package Organization:
- **Model Layer** (Biru Muda) - Data classes
- **Repository Layer** (Hijau Muda) - Data management
- **Service Layer** (Ungu Muda) - Background services
- **UI Layer** (Orange Muda) - Activities, Fragments, ViewModels
- **Utils Layer** (Abu-abu) - Helper classes
- **Worker Layer** (Ungu Muda) - Background workers
- **Application** (Orange Muda) - Application class

### Key Components:

#### Model Layer
- `Book` - Entitas buku dengan informasi lengkap
- `User` - Data pengguna dengan role admin
- `Transaction` - Transaksi peminjaman buku
- `Notification` - Notifikasi sistem

#### Repository Layer
- `AppRepository` - Central repository dengan caching
- `BookRepository` - Khusus operasi buku dan upload
- `NotificationRepository` - Khusus operasi notifikasi

#### Service Layer
- `NotificationForegroundService` - Background service untuk notifikasi periodik
- `NotificationService` - Logic pembuatan notifikasi
- `MyFirebaseMessagingService` - Handle FCM

#### UI Layer
- `MainActivity` - Activity utama dengan navigation
- Fragment dan ViewModel untuk setiap screen
- Adapter classes untuk RecyclerView

#### Utils Layer
- `PushNotificationHelper` - Helper untuk push notifications
- `LocalNotificationHelper` - Helper untuk local notifications
- Various utility classes

## Architectural Patterns Used

1. **MVVM (Model-View-ViewModel)** - Separation of concerns
2. **Repository Pattern** - Centralized data management
3. **Observer Pattern** - LiveData untuk reactive UI
4. **Singleton Pattern** - Firebase instances
5. **Factory Pattern** - ViewModel creation
6. **Strategy Pattern** - Different notification strategies

## Technologies Integrated

- **Firebase Firestore** - Database
- **Firebase Authentication** - User management
- **Firebase Cloud Messaging** - Push notifications
- **Firebase Storage** - File storage
- **Android Architecture Components** - LiveData, ViewModel
- **Kotlin Coroutines** - Asynchronous programming
- **WorkManager** - Background tasks
- **AlarmManager** - Scheduled notifications

## Color Coding

- 🔵 **Model Layer** (#E1F5FE) - Data entities
- 🟢 **Repository Layer** (#E8F5E8) - Data access
- 🟣 **Service Layer** (#F3E5F5) - Background services
- 🟠 **UI Layer** (#FFF3E0) - User interface
- ⚪ **Utils Layer** (#FAFAFA) - Utilities

## Notes

- Diagram menunjukkan struktur high-level aplikasi
- Beberapa method parameters disingkat untuk readability
- Relationships menunjukkan dependencies utama
- Private methods ditandai dengan `-`, public dengan `+`
- Static methods ditandai dengan `{static}`

## Customization

Anda bisa memodifikasi file `.puml` untuk:
- Menambah/mengurangi detail method
- Mengubah warna package
- Menambah class baru
- Mengubah relationship
- Menambah notes atau comments

Setelah modifikasi, generate ulang gambar menggunakan salah satu cara di atas.