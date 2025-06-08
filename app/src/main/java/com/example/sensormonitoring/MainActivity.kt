package com.example.sensormonitoring

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.db.williamchart.Labels
import com.example.sensormonitoring.databinding.ActivityGasBinding
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGasBinding
    private lateinit var databaseRef: DatabaseReference
    private lateinit var historyRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityGasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = FirebaseDatabase.getInstance()
        databaseRef = database.getReference("ESP32")
        historyRef = database.getReference("History_Gas")

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi gauge
        binding.halfGauge.apply {
            startValue = 0
            endValue = 600
            startAngle = 140
            sweepAngle = 260
            value = 0
        }

        // Set tampilan awal
        binding.valueHalfGauge.text = binding.halfGauge.value.toString()
        updateIndicator(binding.halfGauge.value)

        listenForSensorData()
    }

    private fun listenForSensorData() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val gasAnalog = snapshot.child("Gas_Analog").getValue(Int::class.java)

                if (gasAnalog != null) {
                    val clampedValue = gasAnalog.coerceIn(binding.halfGauge.startValue, binding.halfGauge.endValue)
                    binding.halfGauge.value = clampedValue
                    binding.valueHalfGauge.text = clampedValue.toString()
                    updateIndicator(clampedValue)

                    // Simpan ke History_Gas dengan waktu sekarang
                    val currentTime = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
                    val data = mapOf("time" to currentTime, "value" to clampedValue)
                    historyRef.push().setValue(data)

                    // Tampilkan ke chart
                    loadHistoryToChart()
                } else {
                    Log.w("Firebase", "Gas_Analog is null")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to read value.", error.toException())
            }
        })
    }

    private fun updateIndicator(currentValue: Int) {
        val (iconResId, statusText) = when (currentValue) {
            in 0..199 -> R.drawable.siaga to "Lingkungan Aman"
            in 200..399 -> R.drawable.waspada to "Perlu Kewaspadaan!"
            in 400..600 -> R.drawable.bahaya to "Kondisi Berbahaya!"
            else -> R.drawable.siaga to "Status Tidak Diketahui"
        }

        // Pastikan ID ini sesuai dengan di layout XML
        binding.iconIncicator.setImageResource(iconResId)
        binding.titleIndicator.text = statusText
    }

    private fun loadHistoryToChart() {
        historyRef.limitToLast(10).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val barData = mutableListOf<Pair<String, Float>>()

                for (dataSnapshot in snapshot.children) {
                    val time = dataSnapshot.child("time").getValue(String::class.java)
                    val value = dataSnapshot.child("value").getValue(Int::class.java)

                    if (time != null && value != null) {
                        val label = time.split(" ").getOrNull(1) ?: time // ambil jam saja
                        barData.add(label to value.toFloat())
                    }
                }

                // Tampilkan data ke chart
                binding.barChart.apply {
                    chartConfiguration.apply {
                        labelsColor = R.color.white
                    }
                    animate(barData)
                }

                   val Labels = barData.map { it.first }
                    val values = barData.map { it.second }

                    // Tampilkan data ke chart
                    binding.barChart.apply {
                        chartConfiguration.apply {
                            labelsColor = R.color.white
//                            labels = Labels
//                            barsColorsList = List(barData.size) { R.color.white }
                        }
                        animate(barData)
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to load history", error.toException())
            }
        })
    }
}
