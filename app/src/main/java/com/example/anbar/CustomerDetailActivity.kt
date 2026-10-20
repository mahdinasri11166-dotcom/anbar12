package com.example.anbar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.anbar.databinding.ActivityCustomerDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerDetailBinding
    private lateinit var repository: InventoryRepository
    private var customerId: Long = -1
    private var customer: Customer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "پروفایل مشتری"

        repository = InventoryRepository(this)
        customerId = intent.getLongExtra("customer_id", -1L)

        binding.btnReceivePayment.setOnClickListener { showReceiveDialog() }
        binding.btnSendSms.setOnClickListener { sendMessage() }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun load() {
        customer = repository.getCustomer(customerId)
        val c = customer ?: run {
            Toast.makeText(this, "مشتری پیدا نشد", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvDetailName.text = c.name
        binding.tvDetailPhone.text = c.phone.ifBlank { "بدون شماره" }
        binding.tvDetailAddress.text = c.address.ifBlank { "" }
        binding.tvDetailDebt.text = format(c.creditBalance)
        binding.tvDetailPoints.text = c.loyaltyPoints.toString()

        // تاریخچه خرید
        val sales = repository.getCustomerSales(c.id)
        binding.layoutHistory.removeAllViews()
        if (sales.isEmpty()) {
            binding.tvNoHistory.visibility = View.VISIBLE
        } else {
            binding.tvNoHistory.visibility = View.GONE
            val df = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
            sales.forEach { s ->
                val tv = TextView(this).apply {
                    text = "${s.invoiceNumber}  ·  ${df.format(Date(s.timestamp))}\n" +
                            "مبلغ: ${format(s.total)}" +
                            if (s.isCredit) "  (نسیه)" else "  (نقدی)"
                    textSize = 13f
                    setPadding(12, 10, 12, 10)
                    setBackgroundColor(0xFFFFFFFF.toInt())
                }
                val card = com.google.android.material.card.MaterialCardView(this).apply {
                    radius = 12f
                    cardElevation = 2f
                    useCompatPadding = true
                    addView(tv)
                }
                binding.layoutHistory.addView(card)
            }
        }
    }

    private fun showReceiveDialog() {
        val c = customer ?: return
        if (c.creditBalance <= 0) {
            Toast.makeText(this, "بدهی ندارد", Toast.LENGTH_SHORT).show()
            return
        }
        val et = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "مبلغ دریافتی"
            setText(c.creditBalance.toLong().toString())
            setPadding(40, 30, 40, 30)
        }
        AlertDialog.Builder(this)
            .setTitle("دریافت بدهی — ${c.name}")
            .setView(et)
            .setPositiveButton("ثبت") { _, _ ->
                val amount = et.text.toString().toDoubleOrNull() ?: 0.0
                if (amount <= 0) return@setPositiveButton
                repository.receivePayment(c.id, amount)
                Toast.makeText(this, "پرداخت ثبت شد", Toast.LENGTH_SHORT).show()
                load()
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun sendMessage() {
        val c = customer ?: return
        if (c.phone.isBlank()) {
            Toast.makeText(this, "شماره تماس ثبت نشده", Toast.LENGTH_SHORT).show()
            return
        }
        if (!repository.isSmsEnabled()) {
            Toast.makeText(
                this,
                "ارسال پیام در تنظیمات غیرفعال است. از منو → تنظیمات فعال کنید.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val body = buildString {
            append("سلام ${c.name}،\n")
            if (c.creditBalance > 0) {
                append("مانده بدهی شما: ${format(c.creditBalance)} تومان\n")
            }
            append("امتیاز باشگاه: ${c.loyaltyPoints}")
        }
        // استفاده از Intent پیام‌رسان گوشی (واتساپ، تلگرام، SMS و...)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${c.phone}")
            putExtra("sms_body", body)
        }
        try {
            startActivity(Intent.createChooser(intent, "ارسال پیام از طریق"))
        } catch (e: Exception) {
            // fallback به share عمومی
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
            startActivity(Intent.createChooser(share, "ارسال پیام"))
        }
    }

    private fun format(v: Double): String =
        if (v == v.toLong().toDouble()) String.format("%,d", v.toLong())
        else String.format("%,.0f", v)
}
