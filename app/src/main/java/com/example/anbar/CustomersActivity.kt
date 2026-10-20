package com.example.anbar

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anbar.databinding.ActivityCustomersBinding

class CustomersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomersBinding
    private lateinit var repository: InventoryRepository
    private lateinit var adapter: CustomerAdapter
    private var allCustomers: List<Customer> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.applyLocaleToActivity(this)
        super.onCreate(savedInstanceState)
        binding = ActivityCustomersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarCustomers)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = InventoryRepository(this)
        adapter = CustomerAdapter(emptyList()) { c ->
            startActivity(Intent(this, CustomerDetailActivity::class.java).apply {
                putExtra("customer_id", c.id)
            })
        }
        binding.recyclerCustomers.layoutManager = LinearLayoutManager(this)
        binding.recyclerCustomers.adapter = adapter

        binding.etSearchCustomer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filter(s?.toString().orEmpty())
            }
        })

        binding.btnAddCustomer.setOnClickListener { showAddDialog() }
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
        allCustomers = repository.getCustomers()
        filter(binding.etSearchCustomer.text?.toString().orEmpty())
    }

    private fun filter(q: String) {
        val filtered = if (q.isBlank()) allCustomers
        else allCustomers.filter {
            it.name.contains(q, true) || it.phone.contains(q)
        }
        adapter.update(filtered)
        binding.tvEmptyCustomers.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerCustomers.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddDialog() {
        val etName = EditText(this).apply {
            hint = "نام مشتری"
            setPadding(40, 30, 40, 20)
        }
        val etPhone = EditText(this).apply {
            hint = getString(R.string.phone)
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setPadding(40, 20, 40, 30)
        }
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24, 16, 24, 0)
            addView(etName)
            addView(etPhone)
        }
        AlertDialog.Builder(this)
            .setTitle("افزودن مشتری")
            .setView(layout)
            .setPositiveButton("ذخیره") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, getString(R.string.name_required), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                repository.addCustomer(name, etPhone.text.toString().trim())
                load()
                Toast.makeText(this, "مشتری اضافه شد", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("لغو", null)
            .show()
    }
}
