package com.example.mad_finals_wurmple.mainApp.transactionClasses

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.mad_finals_wurmple.R

class IncomeHistoryManager(private val context: Context, private val view: View) {
    private val tableLayout: TableLayout = view.findViewById(R.id.tableLayout)
    private val spinnerSort: Spinner = view.findViewById(R.id.spinner_sort)
    private val incomeList = mutableListOf<IncomeData>()
    private val db = FirebaseFirestore.getInstance()

    data class IncomeData(
        val transactionId: String = "",
        val name: String = "",
        val amount: Double = 0.0,
        val date: Date = Date()
    )

    init {
        setupSpinner()
        fetchIncomeData()
    }

    private fun setupSpinner() {
        val sortOptions = arrayOf(
            "Name (A-Z)", "Name (Z-A)", "Amount (Low-High)", "Amount (High-Low)", "Date (Recent-Oldest)", "Date (Oldest-Recent)"
        )

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSort.adapter = adapter

        spinnerSort.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                sortData(position)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun fetchIncomeData() {
        incomeList.clear()
        val userId = getCurrentUserId() ?: return

        db.collection("users").document(userId).collection("income")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val incomeData = IncomeData(
                        transactionId = document.id,
                        name = document.getString("name") ?: "",
                        amount = document.getDouble("amount") ?: 0.0,
                        date = document.getTimestamp("date")?.toDate() ?: Date()
                    )
                    incomeList.add(incomeData)
                }
                sortData(4)
            }
            .addOnFailureListener { exception ->
                println("Error getting income data: $exception")
            }
    }

    private fun sortData(sortOption: Int) {
        when (sortOption) {
            0 -> bubbleSortBy { it.name.lowercase() } // Name (A-Z)
            1 -> bubbleSortByDescending { it.name.lowercase() } // Name (Z-A)
            2 -> bubbleSortBy { it.amount } // Amount (Low-High)
            3 -> bubbleSortByDescending { it.amount } // Amount (High-Low)
            4 -> bubbleSortByDescending { it.date.time } // Date (Recent-Oldest)
            5 -> bubbleSortBy { it.date.time } // Date (Oldest-Recent)
        }
        updateTable()
    }

    private fun <T : Comparable<T>> bubbleSortBy(selector: (IncomeData) -> T) {
        val n = incomeList.size
        for (i in 0 until n - 1) {
            for (j in 0 until n - i - 1) {
                if (selector(incomeList[j]) > selector(incomeList[j + 1])) {
                    val temp = incomeList[j]
                    incomeList[j] = incomeList[j + 1]
                    incomeList[j + 1] = temp
                }
            }
        }
    }

    private fun <T : Comparable<T>> bubbleSortByDescending(selector: (IncomeData) -> T) {
        val n = incomeList.size
        for (i in 0 until n - 1) {
            for (j in 0 until n - i - 1) {
                if (selector(incomeList[j]) < selector(incomeList[j + 1])) {
                    val temp = incomeList[j]
                    incomeList[j] = incomeList[j + 1]
                    incomeList[j + 1] = temp
                }
            }
        }
    }

    private fun updateTable() {
        tableLayout.removeViews(1, tableLayout.childCount - 1)
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

        for (income in incomeList) {
            val tableRow = TableRow(context)
            tableRow.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            tableRow.addView(createTableCell(income.name))
            tableRow.addView(createTableCell(String.format("$%.2f", income.amount)))
            tableRow.addView(createTableCell(dateFormat.format(income.date)))

            tableLayout.addView(tableRow)
        }
    }

    private fun createTableCell(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setPadding(8, 8, 8, 8)
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(context, R.drawable.table_border)
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            setTextColor(ContextCompat.getColor(context, R.color.black))
        }
    }

    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }
}
