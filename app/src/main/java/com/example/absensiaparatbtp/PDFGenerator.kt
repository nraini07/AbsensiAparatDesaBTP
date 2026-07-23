package com.example.absensiaparatbtp

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PDFGenerator(private val context: Context) {

    fun generateLaporanAbsensiPDF(
        data: List<LaporanRingkasanAbsensi>,
        bulan: String,
        tahun: Int
    ) {
        try {
            // Direktori Downloads/Laporan_Absensi
            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Laporan_Absensi"
            )
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            // Nama file
            val timestamp =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Laporan_Absensi_${bulan}_${tahun}_$timestamp.pdf"
            val file = File(downloadDir, fileName)

            // Buat PDF
            val writer = PdfWriter(file)
            val pdfDocument = PdfDocument(writer)
            val document = Document(pdfDocument)

            // =======================
            // KOP SURAT & JUDUL
            // =======================

            // Kop: PRESENSI APARAT...
            document.add(
                Paragraph("PRESENSI APARAT DESA BATU PANNU")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14f)
                    .setBold()
            )

            document.add(
                Paragraph("KECAMATAN MAMUJU")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12f)
                    .setBold()
            )

            // Spasi kecil
            document.add(
                Paragraph("")
                    .setMarginTop(5f)
            )

            // Judul laporan
            document.add(
                Paragraph("LAPORAN ABSENSI PEGAWAI")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(13f)
                    .setBold()
            )

            // Periode
            document.add(
                Paragraph("Bulan: $bulan $tahun")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(11f)
                    .setMarginTop(5f)
                    .setMarginBottom(15f)
            )

            // =======================
            // TABEL DATA + TOTAL
            // =======================

            // 6 kolom: No, Nama, Masuk, Izin, Sakit, Keluar
            val table = Table(
                floatArrayOf(
                    1f,  // No
                    4f,  // Nama Pegawai
                    2f,  // Masuk
                    2f,  // Izin
                    2f,  // Sakit
                    2f   // Keluar
                )
            )
            table.setWidth(UnitValue.createPercentValue(100f))

            // Header
            val headers = arrayOf("No", "Nama Pegawai", "Masuk", "Izin", "Sakit", "Keluar")
            for (header in headers) {
                table.addHeaderCell(
                    Cell()
                        .add(
                            Paragraph(header)
                                .setBold()
                                .setTextAlignment(TextAlignment.CENTER)
                        )
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                )
            }

            // Hitung total
            val totalMasuk = data.sumOf { it.masuk }
            val totalIzin = data.sumOf { it.izin }
            val totalSakit = data.sumOf { it.sakit }
            val totalKeluar = data.sumOf { it.keluar }

            // Data pegawai
            for ((index, item) in data.withIndex()) {
                table.addCell(
                    Cell().add(
                        Paragraph((index + 1).toString())
                            .setTextAlignment(TextAlignment.CENTER)
                    )
                )
                table.addCell(
                    Cell().add(
                        Paragraph(item.namaPegawai)
                            .setTextAlignment(TextAlignment.LEFT)
                    )
                )
                table.addCell(
                    Cell().add(
                        Paragraph(item.masuk.toString())
                            .setTextAlignment(TextAlignment.CENTER)
                    )
                )
                table.addCell(
                    Cell().add(
                        Paragraph(item.izin.toString())
                            .setTextAlignment(TextAlignment.CENTER)
                    )
                )
                table.addCell(
                    Cell().add(
                        Paragraph(item.sakit.toString())
                            .setTextAlignment(TextAlignment.CENTER)
                    )
                )
                table.addCell(
                    Cell().add(
                        Paragraph(item.keluar.toString())
                            .setTextAlignment(TextAlignment.CENTER)
                    )
                )
            }

            // Baris TOTAL
            table.addCell(
                Cell(1, 2) // merge 2 kolom: No + Nama
                    .add(
                        Paragraph("TOTAL")
                            .setBold()
                            .setTextAlignment(TextAlignment.CENTER)
                    )
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            )
            table.addCell(
                Cell().add(
                    Paragraph(totalMasuk.toString())
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                ).setBackgroundColor(ColorConstants.LIGHT_GRAY)
            )
            table.addCell(
                Cell().add(
                    Paragraph(totalIzin.toString())
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                ).setBackgroundColor(ColorConstants.LIGHT_GRAY)
            )
            table.addCell(
                Cell().add(
                    Paragraph(totalSakit.toString())
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                ).setBackgroundColor(ColorConstants.LIGHT_GRAY)
            )
            table.addCell(
                Cell().add(
                    Paragraph(totalKeluar.toString())
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                ).setBackgroundColor(ColorConstants.LIGHT_GRAY)
            )

            document.add(table)

            // =======================
            // FOOTER / TANDA TANGAN
            // =======================

            // Tanggal cetak (untuk info)
            val tanggalCetak =
                SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())

            document.add(
                Paragraph("Dicetak: $tanggalCetak")
                    .setTextAlignment(TextAlignment.LEFT)
                    .setFontSize(9f)
                    .setMarginTop(15f)
            )

            // Ruang untuk footer kanan
            document.add(Paragraph("\n\n"))

            // Footer kanan bawah: kantor + bulan/tahun + tanda tangan
            val footer = Paragraph()
                .add("KANTOR DESA BATU PANNU\n")
                .add("Batu Pannu, $bulan $tahun\n\n\n\n")
                .add("Kepala Desa Batu Pannu\n\n")
                .add("________________________")
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(11f)

            document.add(footer)

            // Tutup dokumen
            document.close()

            Toast.makeText(
                context,
                "PDF berhasil disimpan di: ${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}