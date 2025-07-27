package com.example.mylibraryapps.ui.transaction

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentTransactionListBinding
import com.example.mylibraryapps.model.Transaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
//import com.itextpdf.layout.property.TextAlignment
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class TransactionListFragment : Fragment() {

    private var _binding: FragmentTransactionListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TransactionAdapter
    private lateinit var viewModel: TransactionViewModel
    private var pendingTransactions: List<Transaction>? = null
    
    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
    }
    
    // Activity result launcher for file picker
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            pendingTransactions?.let { transactions ->
                exportToChosenLocation(transactions, it)
            }
        }
        pendingTransactions = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionListBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupFab()
    }

//    private fun setupToolbar() {
//        binding.toolbar.setNavigationOnClickListener {
//            requireActivity().onBackPressed()
//        }
//
//        binding.toolbar.setOnMenuItemClickListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.action_filter_all -> {
//                    viewModel.refreshTransactions()
//                    true
//                }
//                R.id.action_filter_borrow -> {
//                    viewModel.refreshTransactions("sedang dipinjam")
//                    true
//                }
//                R.id.action_filter_return -> {
//                    viewModel.refreshTransactions("sudah dikembalikan")
//                    true
//                }
//                R.id.action_export_pdf -> {
//                    showExportOptions()
//                    true
//                }
//                else -> false
//            }
//        }
//    }
//
    private fun setupFab() {
        binding.actionExportPdf.setOnClickListener {
            showExportOptions()
        }
    }

    private fun showExportOptions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ekspor Data")
            .setItems(arrayOf("Export PDF - Pilih Lokasi", "Export PDF - Otomatis ke Downloads")) { _, which ->
                when (which) {
                    0 -> exportToPdfWithLocationPicker()
                    1 -> exportToPdf()
                }
            }
            .show()
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter { transaction ->
            if (transaction.id.isNotEmpty()) {
                navigateToDetail(transaction)
            } else {
                showToast("Transaksi tidak valid")
            }
        }

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TransactionListFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                showError(it)
                viewModel.clearErrorMessage()
            }
        }

        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions.isEmpty()) {
                showEmptyState(null)
            } else {
                val sortedList = transactions.sortedByDescending { parseDate(it.borrowDate) }
                adapter.submitList(sortedList)
                binding.tvEmpty.visibility = View.GONE
            }
        }

        // Observe admin status to show/hide export FAB
        viewModel.isAdmin.observe(viewLifecycleOwner) { isAdmin ->
            binding.actionExportPdf.visibility = if (isAdmin) View.VISIBLE else View.GONE
        }
    }

    private fun exportToPdfWithLocationPicker() {
        val transactions = viewModel.transactions.value ?: emptyList()
        
        if (transactions.isEmpty()) {
            showToast("Tidak ada data transaksi untuk diekspor")
            return
        }
        
        // Store transactions temporarily
        pendingTransactions = transactions
        
        // Create filename with timestamp
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "LaporanTransaksi_$timeStamp.pdf"
        
        // Launch file picker
        createDocumentLauncher.launch(fileName)
    }

    private fun exportToChosenLocation(transactions: List<Transaction>, uri: Uri) {
        try {
            binding.progressBar.visibility = View.VISIBLE
            
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                val pdfWriter = PdfWriter(outputStream)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)

                generatePdfContent(document, transactions)
                document.close()

                showToast("Laporan PDF berhasil disimpan di lokasi yang dipilih!")
                openPdfFileFromUri(uri)
            } ?: run {
                showError("Gagal membuka file untuk menulis")
            }
            
        } catch (e: Exception) {
            Log.e("PDF Export", "Error exporting to chosen location", e)
            showError("Gagal mengekspor PDF: ${e.message}")
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun exportToPdf() {
        // Check permission for Android versions before Q (API 29)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
                return
            }
        }

        val transactions = viewModel.transactions.value ?: emptyList()

        if (transactions.isEmpty()) {
            showToast("Tidak ada data transaksi untuk diekspor")
            return
        }

        try {
            // Show progress
            binding.progressBar.visibility = View.VISIBLE
            
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "LaporanTransaksi_$timeStamp.pdf"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+ (API 29+), use MediaStore
                exportToMediaStore(transactions, fileName)
            } else {
                // For older versions, use traditional file system
                exportToLegacyStorage(transactions, fileName)
            }
            
        } catch (e: Exception) {
            Log.e("PDF Export", "Unexpected error", e)
            showError("Terjadi kesalahan: ${e.message}")
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun exportToMediaStore(transactions: List<Transaction>, fileName: String) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requireContext().contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
            } else {
                // For API < 29, fallback to legacy storage
                exportToLegacyStorage(transactions, fileName)
                return
            }

            uri?.let { fileUri ->
                requireContext().contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                    // Create PDF using output stream
                    val pdfWriter = PdfWriter(outputStream)
                    val pdfDocument = PdfDocument(pdfWriter)
                    val document = Document(pdfDocument)

                    generatePdfContent(document, transactions)
                    document.close()

                    showToast("Laporan PDF berhasil disimpan!\nFile disimpan di: Downloads/$fileName")
                    openPdfFileFromUri(fileUri)
                }
            } ?: run {
                showError("Gagal membuat file PDF")
            }
        } catch (e: Exception) {
            Log.e("PDF Export", "Error with MediaStore", e)
            showError("Gagal mengekspor PDF: ${e.message}")
        }
    }

    private fun exportToLegacyStorage(transactions: List<Transaction>, fileName: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, fileName)

            // Initialize PDF writer and document
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            generatePdfContent(document, transactions)
            document.close()

            showToast("Laporan PDF berhasil disimpan!\nFile disimpan di: Downloads/$fileName")
            openPdfFile(file)
        } catch (e: IOException) {
            Log.e("PDF Export", "Error with legacy storage", e)
            showError("Gagal mengekspor PDF: ${e.message}")
        }
    }

    private fun generatePdfContent(document: Document, transactions: List<Transaction>) {
        // Add title
        document.add(
            Paragraph("LAPORAN TRANSAKSI PERPUSTAKAAN")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(16f)
                .setBold()
        )

        // Add date
        val currentDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        document.add(
            Paragraph("Tanggal: $currentDate")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)
        )

        document.add(Paragraph("\n"))

        // Create table with 6 columns
        val table = Table(6)
        table.setWidth(100f)

        // Add table headers
        table.addCell(Paragraph("Nama Peminjam").setBold())
        table.addCell(Paragraph("Judul Buku").setBold())
        table.addCell(Paragraph("Pengarang").setBold())
        table.addCell(Paragraph("Tanggal Pinjam").setBold())
        table.addCell(Paragraph("Tanggal Kembali").setBold())
        table.addCell(Paragraph("Status").setBold())

        // Add transaction data
        transactions.forEach { transaction ->
            table.addCell(Paragraph(transaction.nameUser))
            table.addCell(Paragraph(transaction.title))
            table.addCell(Paragraph(transaction.author))
            table.addCell(Paragraph(transaction.borrowDate))
            table.addCell(Paragraph(transaction.returnDate))
            table.addCell(Paragraph(transaction.status))
        }

        document.add(table)
    }

    private fun openPdfFile(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
        }

        // Verify that there's an app to handle the intent
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            showToast("Tidak ada aplikasi untuk membuka PDF")
        }
    }

    private fun openPdfFileFromUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
        }

        // Verify that there's an app to handle the intent
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            showToast("Tidak ada aplikasi untuk membuka PDF")
        }
    }

    private fun parseDate(dateString: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)?.time ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun showEmptyState(statusFilter: String?) {
        binding.tvEmpty.text = if (statusFilter != null) {
            "Tidak ada transaksi dengan status ini"
        } else {
            "Belum ada transaksi"
        }
        binding.tvEmpty.visibility = View.VISIBLE
    }

    private fun navigateToDetail(transaction: Transaction) {
        val bundle = Bundle().apply {
            putParcelable("transaction", transaction)
        }

        try {
            findNavController().navigate(
                R.id.transactionDetailFragment,
                bundle
            )
        } catch (e: Exception) {
            Log.e("TransactionList", "Navigation error", e)
            showToast("Navigasi gagal: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        binding.tvEmpty.text = message
        binding.tvEmpty.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, try export again
                    exportToPdf()
                } else {
                    // Permission denied
                    showToast("Izin penyimpanan diperlukan untuk mengekspor PDF")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}