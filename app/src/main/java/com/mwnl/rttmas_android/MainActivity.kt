package com.mwnl.rttmas_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mwnl.rttmas_android.core.GUI
import com.mwnl.rttmas_android.core.RTTMAS
import com.mwnl.rttmas_android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var rttmas: RTTMAS


    // GUI references
    lateinit var gui : GUI


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup GUI references
        gui = GUI(
            findViewById(R.id.image_view_detection),
            findViewById(R.id.switch_detection_status),
            findViewById(R.id.text_detection_status),
            findViewById(R.id.text_gps_latitude),
            findViewById(R.id.text_gps_longitude),
            findViewById(R.id.text_speed),
            findViewById(R.id.recycler_view_traffic_alerts),
            findViewById(R.id.warning_gps_unavailable),
            findViewById(R.id.warning_server_unavailable),
            findViewById(R.id.warning_permissions_not_granted),
        )


        // Initialize RTTMAS logic
        rttmas = RTTMAS(this@MainActivity, this, gui)
        rttmas.initialize()
        rttmas.isDetectionEnabled = true
    }


}