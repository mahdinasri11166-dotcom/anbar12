package com.example.anbar

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import java.io.File

/** سفارش ویزیتور — آدرس + درصد تخفیف + چاپ */
class VisitorOrderActivity : AppCompatActivity() {

    private lateinit var repo: InventoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.applyLocaleToActivity(this)
        super.onCreate(savedInstanceState)
        repo = InventoryRepository(this)
        val s = repo.getSettings()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFFF7F9FC.toInt())
        }
        root.addView(TextView(this).apply {
            text = "🛵 سفارش ویزیتور"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        val etVisitor = EditText(this).apply { hint = "نام ویزیتور" }
        val etCustomer = EditText(this).apply { hint = "نام مشتری" }
        val etAddress = EditText(this).apply { hint = "آدرس تحویل" }
        val etPhone = EditText(this).apply {
            hint = "تلفن"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        val etItem = EditText(this).apply { hint = "شرح کالا (دستی)" }
        val etQty = EditText(this).apply {
            hint = "تعداد"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val etPrice = EditText(this).apply {
            hint = "قیمت واحد"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val etDiscount = EditText(this).apply {
            hint = "درصد تخفیف"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText("0")
        }
        listOf(etVisitor, etCustomer, etAddress, etPhone, etItem, etQty, etPrice, etDiscount)
            .forEach { root.addView(it) }

        root.addView(MaterialButton(this).apply {
            text = "ساخت PDF سفارش و اشتراک"
            setOnClickListener {
                val disc = etDiscount.text?.toString()?.toDoubleOrNull() ?: 0.0
                val qty = etQty.text?.toString()?.toDoubleOrNull() ?: 1.0
                val price = etPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
                val sub = qty * price
                val total = sub * (1 - disc / 100.0)
                try {
                    val file = buildPdf(
                        s,
                        etVisitor.text?.toString().orEmpty(),
                        etCustomer.text?.toString().orEmpty().ifEmpty { "مشتری" },
                        etAddress.text?.toString().orEmpty(),
                        etPhone.text?.toString().orEmpty(),
                        etItem.text?.toString().orEmpty().ifEmpty { "کالا" },
                        qty, price, disc, total
                    )
                    val uri = FileProvider.getUriForFile(this@VisitorOrderActivity, "$packageName.provider", file)
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, "اشتراک سفارش"))
                } catch (e: Exception) {
                    Toast.makeText(this@VisitorOrderActivity, e.message, Toast.LENGTH_LONG).show()
                }
            }
        })
        setContentView(ScrollView(this).apply { addView(root) })
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "سفارش ویزیتور"
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun buildPdf(
        s: AppSettings, visitor: String, customer: String, address: String, phone: String,
        item: String, qty: Double, price: Double, disc: Double, total: Double
    ): File {
        val extras = mutableListOf<String>()
        if (visitor.isNotBlank()) extras.add("ویزیتور: $visitor")
        if (address.isNotBlank()) extras.add("آدرس: $address")
        if (phone.isNotBlank()) extras.add("تلفن مشتری: $phone")
        if (disc > 0) extras.add("تخفیف: ${FormatUtils.qty(disc)}٪")
        return InvoicePdfHelper.build(
            context = this,
            settings = s,
            docTitle = "سفارش ویزیتور",
            invoiceNo = "شماره: VO-${System.currentTimeMillis() % 100000}",
            dateText = FormatUtils.dateTime(),
            customerLine = "مشتری: $customer",
            extraLines = extras,
            items = listOf(
                InvoicePdfHelper.Line(
                    name = item.ifBlank { "کالا" },
                    qty = FormatUtils.qty(qty),
                    unitPrice = FormatUtils.money(price),
                    total = FormatUtils.money(qty * price)
                )
            ),
            summaryRows = buildList {
                add("جمع قبل از تخفیف" to FormatUtils.money(qty * price))
                if (disc > 0) add("تخفیف" to "${FormatUtils.qty(disc)}٪")
            },
            grandTotalLabel = "مبلغ نهایی",
            grandTotalValue = FormatUtils.money(total),
            footer = s.invoiceFooter.ifBlank { "از خرید شما متشکریم" },
            verse = MolanaHelper.randomVerse(this),
            filePrefix = "visitor"
        )
    }
}