package com.example.anbar

import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/** شیفت صندوق — شروع با نام متصدی، پایان با آمار فروش/سود/تعداد فاکتور */
class CashShiftActivity : AppCompatActivity() {

    private lateinit var repo: InventoryRepository
    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.applyLocaleToActivity(this)
        super.onCreate(savedInstanceState)
        repo = InventoryRepository(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F7F9FC"))
            setPadding(24, 24, 24, 24)
        }
        root.addView(TextView(this).apply {
            text = "🧾 شیفت صندوق"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        })
        info = TextView(this).apply {
            textSize = 14f
            setLineSpacing(6f, 1f)
            setPadding(0, 0, 0, 16)
        }
        root.addView(info)

        root.addView(MaterialButton(this).apply {
            text = "شروع شیفت"
            setOnClickListener { openShiftDialog() }
        })
        root.addView(MaterialButton(this).apply {
            text = "بستن شیفت و گزارش"
            setBackgroundColor(Color.parseColor("#C62828"))
            setOnClickListener { closeShiftDialog() }
        })

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "شیفت صندوق"
        refresh()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun refresh() {
        val open = repo.getOpenShift()
        if (open == null) {
            info.text = "شیفت بازی نیست.\nبرای شروع، نام متصدی و موجودی اولیه را وارد کنید."
            return
        }
        val sales = repo.getSales().filter {
            it.timestamp >= open.openedAt && it.status == "COMPLETED"
        }
        val salesTotal = sales.sumOf { it.total }
        val profit = sales.sumOf { s ->
            s.items.sumOf { item ->
                val cost = repo.getProduct(item.productId)?.purchasePrice ?: 0.0
                (item.unitPrice - cost) * item.quantity
            }
        }
        info.text = buildString {
            append("متصدی: ${open.openedBy}\n")
            append("شروع: ${FormatUtils.dateTime(open.openedAt)}\n")
            append("موجودی اولیه: ${FormatUtils.money(open.openingCash)}\n\n")
            append("تعداد فاکتور: ${sales.size}\n")
            append("میزان فروش: ${FormatUtils.money(salesTotal)}\n")
            append("سود تقریبی: ${FormatUtils.money(profit)}")
        }
    }

    private fun openShiftDialog() {
        if (repo.getOpenShift() != null) {
            Toast.makeText(this, "شیفت باز از قبل وجود دارد", Toast.LENGTH_SHORT).show()
            return
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 16, 40, 8)
        }
        val etName = EditText(this).apply { hint = "نام متصدی صندوق" }
        val etCash = EditText(this).apply {
            hint = "موجودی اولیه صندوق"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        box.addView(etName)
        box.addView(etCash)
        AlertDialog.Builder(this)
            .setTitle("شروع شیفت")
            .setView(box)
            .setPositiveButton("شروع") { _, _ ->
                val name = etName.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    Toast.makeText(this, "نام متصدی لازم است", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val cash = etCash.text?.toString()?.toDoubleOrNull() ?: 0.0
                repo.openShift(cash, name)
                Toast.makeText(this, "شیفت شروع شد — متصدی: $name", Toast.LENGTH_LONG).show()
                refresh()
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun closeShiftDialog() {
        val open = repo.getOpenShift() ?: run {
            Toast.makeText(this, "شیفت بازی نیست", Toast.LENGTH_SHORT).show()
            return
        }
        val sales = repo.getSales().filter {
            it.timestamp >= open.openedAt && it.status == "COMPLETED"
        }
        val salesTotal = sales.sumOf { it.total }
        val profit = sales.sumOf { s ->
            s.items.sumOf { item ->
                val cost = repo.getProduct(item.productId)?.purchasePrice ?: 0.0
                (item.unitPrice - cost) * item.quantity
            }
        }
        val et = EditText(this).apply {
            hint = "موجودی پایانی صندوق"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        AlertDialog.Builder(this)
            .setTitle("بستن شیفت — ${open.openedBy}")
            .setMessage(
                "تعداد فاکتور: ${sales.size}\n" +
                        "فروش: ${FormatUtils.money(salesTotal)}\n" +
                        "سود تقریبی: ${FormatUtils.money(profit)}"
            )
            .setView(et)
            .setPositiveButton("بستن شیفت") { _, _ ->
                val closing = et.text?.toString()?.toDoubleOrNull() ?: 0.0
                val note = "فاکتور=${sales.size} | فروش=${salesTotal} | سود=$profit | متصدی=${open.openedBy}"
                repo.closeShift(closing, note)
                AlertDialog.Builder(this)
                    .setTitle("خلاصه پایان شیفت")
                    .setMessage(
                        "متصدی: ${open.openedBy}\n" +
                                "تعداد فاکتور: ${sales.size}\n" +
                                "میزان فروش: ${FormatUtils.money(salesTotal)}\n" +
                                "سود تقریبی: ${FormatUtils.money(profit)}\n" +
                                "موجودی پایانی: ${FormatUtils.money(closing)}"
                    )
                    .setPositiveButton("باشه", null)
                    .show()
                refresh()
            }
            .setNegativeButton("لغو", null)
            .show()
    }
}
