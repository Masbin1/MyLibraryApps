package com.example.mylibraryapps.ui.transaction

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionListBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupFab()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_filter_all -> {
                    viewModel.refreshTransactions()
                    true
                }
                R.id.action_filter_borrow -> {
                    viewModel.refreshTransactions("sedang dipinjam")
                    true
                }
                R.id.action_filter_return -> {
                    viewModel.refreshTransactions("sudah dikembalikan")
                    true
                }
                R.id.action_export_pdf -> {
                    exportToPdf()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFab() {
        binding.actionExportPdf.setOnClickListener {
            showExportOptions()
        }
    }

    private fun showExportOptions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ekspor Data")
            .setItems(arrayOf("Export PDF")) { _, which ->
                when (which) {
                    0 -> exportToPdf()
                    1 -> binding.toolbar.showOverflowMenu()
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
    }

    private fun exportToPdf() {
        val transactions = viewModel.transactions.value ?: emptyList()

        if (transactions.isEmpty()) {
            showToast("Tidak ada data transaksi untuk diekspor")
            return
        }

        try {
            // Create a file in the Downloads directory
            val downloadsDir = requireContext().getExternalFilesDir(null)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "LaporanTransaksi_$timeStamp.pdf"
            val file = File(downloadsDir, fileName)

            // Initialize PDF writer and document
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

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
            document.close()

            // Show success message
            showToast("Laporan PDF berhasil disimpan")

            // Open the PDF file
            openPdfFile(file)
        } catch (e: IOException) {
            Log.e("PDF Export", "Error exporting PDF", e)
            showError("Gagal mengekspor PDF: ${e.message}")
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}