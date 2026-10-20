package com.example.anbar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.applyLocaleToActivity(this)
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFFF7F9FC.toInt())
        }

        root.addView(TextView(this).apply {
            text = "کالانو"
            textSize = 28f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF1565C0.toInt())
        })
        root.addView(TextView(this).apply {
            text = "نسخه ۴.۶.۰ · مدیریت هوشمند انبار و فروش"
            textSize = 13f
            setTextColor(0xFF5A6A7A.toInt())
            setPadding(0, 4, 0, 16)
        })

        val card = MaterialCardView(this).apply {
            radius = 16f
            cardElevation = 2f
            setCardBackgroundColor(0xFFFFFFFF.toInt())
            setContentPadding(20, 20, 20, 20)
        }
        val body = TextView(this).apply {
            text = buildString {
                append("کالانو برای مغازه‌داران، انبارداران و تیم‌های فروش طراحی شده است.\n\n")
                append("• انبار چندگانه، ورود/خروج، اسکن بارکد\n")
                append("• صندوق فروش، پیش‌فاکتور، سفارش ویزیتور\n")
                append("• شیفت صندوق با نام متصدی و گزارش سود\n")
                append("• چندارزی، هزینه و سود خالص، بکاپ امن\n")
                append("• فارسی · انگلیسی · عربی\n\n")
                append("سازنده: مهدی طریری\n")
                append("پشتیبانی: mahditariri@gmail.com")
            }
            textSize = 14f
            setLineSpacing(8f, 1f)
            setTextColor(0xFF1A2332.toInt())
        }
        card.addView(body)
        root.addView(card)

        root.addView(MaterialButton(this).apply {
            text = "ارسال ایمیل پشتیبانی"
            setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:mahditariri@gmail.com")).apply {
                        putExtra(Intent.EXTRA_SUBJECT, "پشتیبانی کالانو")
                    })
                } catch (_: Exception) { }
            }
        })
        root.addView(MaterialButton(this).apply {
            text = MolanaHelper.randomVerse(this@AboutActivity)
            textSize = 12f
            isAllCaps = false
        })

        setContentView(ScrollView(this).apply { addView(root) })
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "درباره کالانو"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
