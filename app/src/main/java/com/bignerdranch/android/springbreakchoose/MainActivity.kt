package com.bignerdranch.android.springbreakchoose

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import com.bignerdranch.android.springbreakchoose.databinding.ActivityMainBinding
import com.google.mlkit.common.model.DownloadConditions
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.speech.RecognizerIntent
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import java.util.Locale
import java.util.Objects
import kotlin.math.sqrt

const val REQUEST_CODE = 200
class MainActivity : AppCompatActivity() {
    lateinit var editText: EditText
    private lateinit var binding: ActivityMainBinding
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false
    private lateinit var recorder: MediaRecorder
    private var conditions = DownloadConditions.Builder().requireWifi().build()
    private val REQUEST_CODE_SPEECH_INPUT = 1
    private var language: String = ""
    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    private lateinit var mediaPlayer: MediaPlayer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionGranted= ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED
        if(!permissionGranted)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
        editText = findViewById(R.id.editText)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        Objects.requireNonNull(sensorManager)!!
            .registerListener(sensorListener, sensorManager!!
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)

        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH
        mediaPlayer = MediaPlayer()
        val spinner = binding.spinner
        val record = binding.recordBtn

        val languages = resources.getStringArray(R.array.languages)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                language = parent?.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                language = "English"
            }
        }

        record.setOnClickListener {
            startRecording()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == REQUEST_CODE)
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun startRecording() {
        if(!permissionGranted){
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            return
        }
        // start recording
        // on below line we are calling speech recognizer intent.
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault()
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLanguageCode(language))
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            // on below line we are displaying error message in toast
            Toast
                .makeText(
                    this@MainActivity, " " + e.message,
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    }

    private fun getLanguageCode(language: String): String {
        return when (language) {
            "English" -> "en"
            "Chinese" -> "zh"
            "Spanish" -> "es"
            "French" -> "fr"
            "Japanese" -> "ja"
            else -> Locale.getDefault().language // Return device's default language if not found
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            // on below line we are checking if result code is ok
            if (resultCode == RESULT_OK && data != null) {

                val res: ArrayList<String> =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                editText.setText(
                    Objects.requireNonNull(res)[0]
                )
            }
        }
    }

    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {

            // Fetching x,y,z values
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration

            // Getting current accelerations
            // with the help of fetched x,y,z values
            currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            // Display a Toast message if
            // acceleration value is over 12
            if (acceleration > 12) {
                Toast.makeText(applicationContext, "Shake event detected", Toast.LENGTH_SHORT).show()
                playAudioForLanguage(language)

                val locationUrl = when (language) {
                    "English" -> "40.6958091,-74.6035277"
                    "Chinese" -> "39.938417,116.0678134"
                    "French" -> "48.8588255,2.2646345"
                    "Japanese" -> "35.5020584,138.4504996"
                    "Spanish" -> "19.3906594,-99.308425"
                    else -> return
                }
                // Start the MapsActivity
                val intent = Intent(this@MainActivity, MapsActivity::class.java)
                intent.putExtra("LOCATION_URL", locationUrl)
                startActivity(intent)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun playAudioForLanguage(language: String) {
        val resourceId = when (language) {
            "English" -> R.raw.en
            "Chinese" -> R.raw.zh
            "French" -> R.raw.fr
            "Japanese" -> R.raw.ja
            "Spanish" -> R.raw.es
            else -> return
        }
        mediaPlayer.release()
        mediaPlayer = MediaPlayer.create(this, resourceId)
        mediaPlayer.start()
    }

    override fun onResume() {
        sensorManager?.registerListener(sensorListener, sensorManager!!.getDefaultSensor(
            Sensor .TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL
        )
        super.onResume()
    }

    override fun onPause() {
        sensorManager!!.unregisterListener(sensorListener)
        super.onPause()
    }
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

}
