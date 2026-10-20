package com.example.anbar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.anbar.databinding.ActivityPurchaseBinding

class PurchaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPurchaseBinding
    private lateinit var repository: InventoryRepository
    private var suppliers: List<Supplier> = emptyList()
    private var warehouses: List<Warehouse> = emptyList()
    private var products: List<Product> = emptyList()
    private var selectedSupplier: Supplier? = null
    private var selectedWarehouse: Warehouse? = null
    private var selectedProduct: Product? = null
    private val items = mutableListOf<PurchaseItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPurchaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "فاکتور خرید"

        repository = InventoryRepository(this)
        suppliers = repository.getSuppliers()
        warehouses = repository.getWarehouses()
        products = repository.getProducts()

        binding.actvSupplier.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, suppliers.map { it.name })
        )
        binding.actvSupplier.setOnClickListener { binding.actvSupplier.showDropDown() }
        binding.actvSupplier.setOnItemClickListener { _, _, pos, _ -> selectedSupplier = suppliers[pos] }

        binding.actvWarehouse.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, warehouses.map { it.name })
        )
        binding.actvWarehouse.setOnClickListener { binding.actvWarehouse.showDropDown() }
        binding.actvWarehouse.setOnItemClickListener { _, _, pos, _ -> selectedWarehouse = warehouses[pos] }
        if (warehouses.isNotEmpty()) {
            selectedWarehouse = warehouses.first { it.isDefault }
            binding.actvWarehouse.setText(selectedWarehouse!!.name, false)
        }

        binding.actvProduct.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, products.map { it.name })
        )
        binding.actvProduct.setOnClickListener { binding.actvProduct.showDropDown() }
        binding.actvProduct.setOnItemClickListener { _, _, pos, _ -> selectedProduct = products[pos] }

        binding.recyclerPurchaseItems.layoutManager = LinearLayoutManager(this)
        refreshItems()

        binding.btnAddItem.setOnClickListener { addItem() }
        binding.btnPurchaseCash.setOnClickListener { complete(isCredit = false) }
        binding.btnPurchaseCredit.setOnClickListener { complete(isCredit = true) }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun addItem() {
        val p = selectedProduct
        if (p == null) {
            Toast.makeText(this, "کالا را انتخاب کنید", Toast.LENGTH_SHORT).show()
            return
        }
        val qty = binding.etQty.text?.toString()?.toDoubleOrNull() ?: 0.0
        val cost = binding.etCost.text?.toString()?.toDoubleOrNull() ?: 0.0
        if (qty <= 0 || cost < 0) {
            Toast.makeText(this, "تعداد و قیمت نامعتبر", Toast.LENGTH_SHORT).show()
            return
        }
        items.add(
            PurchaseItem(
                productId = p.id,
                productName = p.name,
                quantity = qty,
                unitCost = cost,
                lineTotal = qty * cost
            )
        )
        binding.etQty.setText("1")
        binding.etCost.text?.clear()
        binding.actvProduct.setText("", false)
        selectedProduct = null
        refreshItems()
    }

    private fun refreshItems() {
        binding.recyclerPurchaseItems.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply {
                    setPadding(24, 16, 24, 16)
                    textSize = 14f
                }
                return object : RecyclerView.ViewHolder(tv) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val it = items[position]
                (holder.itemView as TextView).text =
                    "${it.productName}  ${format(it.quantity)} × ${format(it.unitCost)} = ${format(it.lineTotal)}"
            }

            override fun getItemCount() = items.size
        }
        binding.tvPurchaseTotal.text = "جمع: ${format(items.sumOf { it.lineTotal })}"
    }

    private fun complete(isCredit: Boolean) {
        val supplier = selectedSupplier
        val warehouse = selectedWarehouse
        if (supplier == null || warehouse == null) {
            Toast.makeText(this, "تامین‌کننده و انبار را انتخاب کنید", Toast.LENGTH_SHORT).show()
            return
        }
        if (items.isEmpty()) {
            Toast.makeText(this, "حداقل یک کالا اضافه کنید", Toast.LENGTH_SHORT).show()
            return
        }
        val total = items.sumOf { it.lineTotal }
        val paid = if (isCredit) 0.0 else total
        val result = repository.completePurchase(
            supplierId = supplier.id,
            warehouseId = warehouse.id,
            items = items.toList(),
            paidAmount = paid,
            isCredit = isCredit
        )
        if (result != null) {
            WebhookSender.notifyPurchase(this, result)
            Toast.makeText(this, "فاکتور خرید ثبت شد — موجودی افزایش یافت", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Toast.makeText(this, "خطا در ثبت", Toast.LENGTH_SHORT).show()
        }
    }

    private fun format(v: Double): String =
        if (v == v.toLong().toDouble()) String.format("%,d", v.toLong())
        else String.format("%,.0f", v)
}
