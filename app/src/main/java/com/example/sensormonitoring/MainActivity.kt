package com.example.sensormonitoring

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sensormonitoring.databinding.ActivityMainBinding
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // inisialisasi binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // atur padding untuk sistem bar (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // inisialisasi database Firebase
        database = FirebaseDatabase
            .getInstance("https://webesp32-83d35-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("ESP32")

        listenForSensorData()
    }

    private fun listenForSensorData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val suhu = snapshot.child("Suhu").getValue(Float::class.java)
                val kelembapan = snapshot.child("Kelembapan").getValue(Float::class.java)
                val motion = snapshot.child("PIR").getValue(Int::class.java)
                val gasAnalog = snapshot.child("Gas_Analog").getValue(Int::class.java)
                val statusGas = snapshot.child("Status_Gas").getValue(String::class.java)

                // update UI pakai binding
                binding.tvSuhu.text = suhu?.let { "$it °C" } ?: "-- °C"
                binding.tvKelembapan.text = kelembapan?.let { "$it %" } ?: "-- %"
                binding.tvMotion.text = if (motion == 1) "Gerakan Terdeteksi" else "Tidak Ada Gerakan"
                binding.tvGasAnalog.text = "Analog: ${gasAnalog ?: "--"}"
                binding.tvGasStatus.text = "Status: ${statusGas ?: "--"}"
            }

            override fun onCancelled(error: DatabaseError) {
                // log error kalau perlu
            }
        })
    }
}
