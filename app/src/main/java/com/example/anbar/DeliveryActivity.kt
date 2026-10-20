package com.example.anbar

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * تحویل به پیک با وضعیت:
 * آماده → در مسیر → تحویل‌شده
 */
class DeliveryActivity : AppCompatActivity() {

    private lateinit var repository: InventoryRepository
    private lateinit var listLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.applyLocaleToActivity(this)
        super.onCreate(savedInstanceState)
        repository = InventoryRepository(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F6F9"))
            setPadding(16, 16, 16, 16)
        }

        root.addView(TextView(this).apply {
            text = "🚚 تحویل پیک"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 4)
        })
        root.addView(TextView(this).apply {
            text = "وضعیت‌ها: آماده → در مسیر → تحویل‌شده"
            textSize = 12f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 0, 0, 12)
        })

        root.addView(MaterialButton(this).apply {
            text = "＋ سفارش تحویل جدید"
            setBackgroundColor(Color.parseColor("#FF6D00"))
            setTextColor(Color.WHITE)
            setOnClickListener { showAddDialog() }
        })

        // فیلتر وضعیت
        val filterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 8)
        }
        var filter = "ALL"
        fun refreshWithFilter() {
            refresh(filter)
        }
        listOf(
            "همه" to "ALL",
            "آماده" to "READY",
            "در مسیر" to "IN_TRANSIT",
            "تحویل" to "DELIVERED"
        ).forEach { (label, code) ->
            filterRow.addView(MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = label
                textSize = 11f
                setOnClickListener {
                    filter = code
                    refreshWithFilter()
                }
            })
        }
        root.addView(filterRow)

        val scroll = ScrollView(this)
        listLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(listLayout)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "تحویل پیک"
        refresh("ALL")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun statusColor(status: String): Int = when (status) {
        "READY" -> Color.parseColor("#1565C0")
        "IN_TRANSIT" -> Color.parseColor("#F9A825")
        "DELIVERED" -> Color.parseColor("#2E7D32")
        else -> Color.GRAY
    }

    private fun refresh(filter: String) {
        listLayout.removeAllViews()
        var items = repository.getDeliveries()
        if (filter != "ALL") items = items.filter { it.status == filter }
        items = items.sortedByDescending { it.createdAt }

        if (items.isEmpty()) {
            listLayout.addView(TextView(this).apply {
                text = "موردی نیست"
                setTextColor(Color.GRAY)
                setPadding(8, 32, 8, 8)
                gravity = Gravity.CENTER
            })
            return
        }

        items.forEach { d ->
            val card = MaterialCardView(this).apply {
                radius = 16f
                cardElevation = 3f
                useCompatPadding = true
                setCardBackgroundColor(Color.WHITE)
            }
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 20, 24, 20)
            }

            // badge + name
            val head = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            head.addView(TextView(this).apply {
                text = d.statusLabel()
                textSize = 11f
                setTextColor(Color.WHITE)
                setPadding(16, 6, 16, 6)
                background = GradientDrawable().apply {
                    cornerRadius = 20f
                    setColor(statusColor(d.status))
                }
            })
            head.addView(TextView(this).apply {
                text = "  ${d.customerName}"
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
            })
            box.addView(head)

            box.addView(TextView(this).apply {
                text = buildString {
                    if (d.address.isNotBlank()) append("📍 ${d.address}\n")
                    if (d.customerPhone.isNotBlank()) append("📞 ${d.customerPhone}\n")
                    if (d.courierName.isNotBlank()) append("🛵 پیک: ${d.courierName}\n")
                    append("💰 ${FormatUtils.money(d.total)}")
                    if (d.note.isNotBlank()) append("\n📝 ${d.note}")
                }
                textSize = 13f
                setTextColor(Color.parseColor("#64748B"))
                setPadding(0, 10, 0, 10)
                setLineSpacing(4f, 1f)
            })

            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            fun statusBtn(label: String, status: String) {
                if (d.status == status) return
                row.addView(MaterialButton(this@DeliveryActivity, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = label
                    textSize = 11f
                    setTextColor(statusColor(status))
                    setOnClickListener {
                        repository.updateDeliveryStatus(d.id, status)
                        Toast.makeText(this@DeliveryActivity, "وضعیت: $label", Toast.LENGTH_SHORT).show()
                        refresh(filter)
                    }
                })
            }
            statusBtn("آماده", "READY")
            statusBtn("در مسیر", "IN_TRANSIT")
            statusBtn("تحویل شد", "DELIVERED")
            box.addView(row)
            card.addView(box)
            listLayout.addView(card)
        }
    }

    private fun showAddDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 16, 40, 8)
        }
        fun field(hint: String, inputType: Int = android.text.InputType.TYPE_CLASS_TEXT) =
            EditText(this).apply {
                this.hint = hint
                this.inputType = inputType
            }.also { layout.addView(it) }

        val etName = field("نام مشتری")
        val etPhone = field("موبایل", android.text.InputType.TYPE_CLASS_PHONE)
        val etAddress = field("آدرس تحویل")
        val etCourier = field("نام پیک")
        val etTotal = field(
            "مبلغ",
            android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        val etNote = field("یادداشت")

        AlertDialog.Builder(this)
            .setTitle("سفارش تحویل جدید")
            .setView(layout)
            .setPositiveButton("ثبت") { _, _ ->
                val name = etName.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    Toast.makeText(this, "نام مشتری لازم است", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                repository.addDelivery(
                    name,
                    etPhone.text?.toString().orEmpty(),
                    etAddress.text?.toString().orEmpty(),
                    etCourier.text?.toString().orEmpty(),
                    etTotal.text?.toString()?.toDoubleOrNull() ?: 0.0,
                    etNote.text?.toString().orEmpty()
                )
                Toast.makeText(this, "ثبت شد — وضعیت: آماده", Toast.LENGTH_SHORT).show()
                refresh("ALL")
            }
            .setNegativeButton("لغو", null)
            .show()
    }
}
