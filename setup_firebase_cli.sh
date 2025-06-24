#!/bin/bash

# Script untuk menginstal dan mengatur Firebase CLI
# Untuk pengembangan MyLibraryApps

echo "=== Instalasi Firebase CLI ==="
echo "Memeriksa apakah Node.js sudah terinstal..."

# Periksa apakah Node.js sudah terinstal
if ! command -v node &> /dev/null; then
    echo "Node.js tidak ditemukan. Silakan instal Node.js terlebih dahulu."
    echo "Kunjungi https://nodejs.org/ untuk mengunduh dan menginstal."
    exit 1
fi

echo "Node.js ditemukan. Versi: $(node -v)"

# Periksa apakah Firebase CLI sudah terinstal
if ! command -v firebase &> /dev/null; then
    echo "Firebase CLI tidak ditemukan. Menginstal Firebase CLI..."
    npm install -g firebase-tools
else
    echo "Firebase CLI sudah terinstal. Versi: $(firebase --version)"
    echo "Memperbarui Firebase CLI..."
    npm update -g firebase-tools
fi

echo "=== Login ke Firebase ==="
echo "Silakan login ke akun Firebase Anda..."
firebase login

echo "=== Inisialisasi Proyek Firebase ==="
echo "Menginisialisasi proyek Firebase di direktori ini..."
firebase init firestore

echo "=== Mengimpor Aturan dan Indeks ==="
echo "Mengimpor aturan dan indeks dari file konfigurasi..."

# Periksa apakah file konfigurasi ada
if [ -f "firestore.rules" ] && [ -f "firestore.indexes.json" ]; then
    echo "File konfigurasi ditemukan. Siap untuk deploy."
else
    echo "File konfigurasi tidak ditemukan. Silakan buat file firestore.rules dan firestore.indexes.json terlebih dahulu."
    exit 1
fi

echo "=== Deploy Aturan dan Indeks ==="
echo "Apakah Anda ingin men-deploy aturan dan indeks sekarang? (y/n)"
read -r deploy_now

if [ "$deploy_now" = "y" ] || [ "$deploy_now" = "Y" ]; then
    echo "Men-deploy aturan dan indeks..."
    firebase deploy --only firestore:rules,firestore:indexes
    echo "Deploy selesai!"
else
    echo "Untuk men-deploy nanti, gunakan perintah:"
    echo "firebase deploy --only firestore:rules,firestore:indexes"
fi

echo "=== Selesai ==="
echo "Firebase CLI telah diatur. Anda dapat menggunakan perintah berikut:"
echo "- firebase deploy --only firestore:rules (untuk men-deploy aturan)"
echo "- firebase deploy --only firestore:indexes (untuk men-deploy indeks)"
echo "- firebase emulators:start (untuk menjalankan emulator lokal)"