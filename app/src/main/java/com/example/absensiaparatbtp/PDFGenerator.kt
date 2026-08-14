package com.example.absensiaparatbtp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.ByteArrayOutputStream
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
            pdfDocument.defaultPageSize = PageSize.A4
            val document = Document(pdfDocument)

            // =======================
            // KOP SURAT: LOGO KIRI + TEKS BENAR-BENAR DI TENGAH HALAMAN
            // =======================
            val lebarLogoKolom = 1.2f
            val kopTable = Table(
                floatArrayOf(
                    lebarLogoKolom, // kolom logo (kiri)
                    4.0f,           // kolom teks kop (tengah)
                    lebarLogoKolom  // kolom kosong (kanan, penyeimbang logo)
                )
            )
            kopTable.setWidth(UnitValue.createPercentValue(100f))
            // WAJIB: kunci lebar kolom (sama seperti tabel data di bawah).
            // Tanpa ini, kolom logo (ada gambar) dan kolom kosong penyeimbang
            // bisa melebar/menyempit otomatis mengikuti isi -- jadi TIDAK
            // simetris walau angkanya sudah sama (1.2f = 1.2f). Inilah
            // penyebab teks kop sebelumnya masih terlihat tidak center/sejajar.
            kopTable.setFixedLayout()
            // Rapatkan jarak vertikal kop ke garis di bawahnya
            kopTable.setMarginBottom(0f)

            // 1) Cell LOGO di kiri -- padding 0 supaya tidak menggeser center teks
            val logoCell = Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0f)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
            try {
                val drawable = ContextCompat.getDrawable(context, R.drawable.logoasli)
                if (drawable != null) {
                    // Diperkecil lagi: target render 110x130px, tampil di 40x48dp
                    val targetWidthPx = 110
                    val targetHeightPx = 130

                    val bitmap = Bitmap.createBitmap(
                        targetWidthPx,
                        targetHeightPx,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, targetWidthPx, targetHeightPx)
                    drawable.draw(canvas)

                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, stream)
                    val imageData = ImageDataFactory.create(stream.toByteArray())
                    val image = Image(imageData)

                    // Logo diperkecil lagi: 40x48 (sebelumnya 60x70)
                    image.scaleToFit(40f, 48f)
                    logoCell.add(image)
                }
            } catch (_: Exception) {
                // kalau logo gagal dimuat, cell dibiarkan kosong
            }
            kopTable.addCell(logoCell)

            // 2) Cell TEKS KOP di tengah -- padding 0 & leading rapat supaya
            // benar-benar center dan tidak ada jarak berlebih ke garis
            val teksKopCell = Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0f)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)

            teksKopCell.add(
                Paragraph("PEMERINTAH KABUPATEN MAMUJU")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14f)
                    .setBold()
                    .setMultipliedLeading(1f)
                    .setMargin(0f)
            )
            teksKopCell.add(
                Paragraph("KECAMATAN MAMUJU")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(13f)
                    .setBold()
                    .setMultipliedLeading(1f)
                    .setMargin(0f)
            )
            teksKopCell.add(
                Paragraph("DESA BATU PANNU")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(13f)
                    .setBold()
                    .setMultipliedLeading(1f)
                    .setMargin(0f)
            )

            kopTable.addCell(teksKopCell)

            // 3) Cell KOSONG kanan (penyeimbang logo, padding 0 juga)
            val spacerCell = Cell().setBorder(Border.NO_BORDER).setPadding(0f)
            kopTable.addCell(spacerCell)

            document.add(kopTable)

            // Garis SATU GARIS LURUS, langsung menempel rapat di bawah teks kop
            val garisKop = Table(floatArrayOf(1f))
            garisKop.setWidth(UnitValue.createPercentValue(100f))
            garisKop.setMarginTop(4f)   // jarak tipis dari teks kop ke garis
            garisKop.setMarginBottom(5f) // jarak tipis dari garis ke "LAPORAN ABSENSI"
            garisKop.addCell(
                Cell()
                    .setHeight(1.5f)
                    .setBackgroundColor(ColorConstants.BLACK)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(0f)
            )
            document.add(garisKop)

            // =======================
            // JUDUL, SUBJUDUL, PERIODE
            // =======================

            document.add(
                Paragraph("LAPORAN ABSENSI")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(11f)
                    .setBold()
                    .setMargin(0f)
            )

            document.add(
                Paragraph("APARAT DESA BATU PANNU")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(9f)
                    .setBold()
                    .setMarginTop(2f)
                    .setMarginBottom(0f)
            )

            document.add(
                Paragraph("Periode: $bulan $tahun")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(11f)
                    .setMarginTop(4f)
                    .setMarginBottom(10f)
            )

            // =======================
            // TABEL DATA + TOTAL
            // Kolom Masuk/Izin/Sakit/Keluar dibuat EQUAL WIDTH (2f semua),
            // sesuai rasio di XML dialog preview (masing-masing 1.1 -- sama rata).
            // =======================

            val lebarNo = 1f
            val lebarNama = 4f
            val lebarKolomStatus = 2f // Masuk, Izin, Sakit, Keluar -- SAMA RATA

            val table = Table(
                floatArrayOf(
                    lebarNo,
                    lebarNama,
                    lebarKolomStatus,
                    lebarKolomStatus,
                    lebarKolomStatus,
                    lebarKolomStatus
                )
            )
            // WAJIB: fixed layout, supaya lebar kolom PATUH ke angka di atas
            // dan tidak auto-resize mengikuti panjang teks (ini penyebab
            // kolom Masuk/Izin/Sakit/Keluar sebelumnya kelihatan tidak rata).
            table.setFixedLayout()
            table.setWidth(UnitValue.createPercentValue(100f))

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

            val totalMasuk = data.sumOf { it.masuk }
            val totalIzin = data.sumOf { it.izin }
            val totalSakit = data.sumOf { it.sakit }
            val totalKeluar = data.sumOf { it.keluar }

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

            table.addCell(
                Cell(1, 2)
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

            val tanggalCetak =
                SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())

            document.add(
                Paragraph("Dicetak: $tanggalCetak")
                    .setTextAlignment(TextAlignment.LEFT)
                    .setFontSize(9f)
                    .setMarginTop(15f)
            )

            document.add(Paragraph("\n\n"))

            val footer = Paragraph()
                .add("KANTOR DESA BATU PANNU\n")
                .add("Batu Pannu, $bulan $tahun\n\n\n\n")
                .add("Kepala Desa Batu Pannu\n\n")
                .add("________________________")
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(11f)

            document.add(footer)

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