package com.ken.space.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.facebook.drawee.view.SimpleDraweeView
import com.ken.space.R
import com.ken.space.model.DetailsViewModel
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.parameter.parametersOf
import java.util.*


const val KEY_LAUNCH_ID = "LAUNCH_ID"

class DetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        val launchId = intent.getStringExtra(KEY_LAUNCH_ID)!!

        val model: DetailsViewModel by viewModel<DetailsViewModel>(
            parameters = { parametersOf(launchId) }
        )

        setupViews(model)

        setupCountdown(model)
    }

    private fun setupViews(model: DetailsViewModel) {
        model.launch.observe(this, Observer { launch ->
            findViewById<SimpleDraweeView>(R.id.image_view).setImageURI(launch.image)
            findViewById<TextView>(R.id.name_text_view).text = launch.name
            findViewById<TextView>(R.id.net_text_view).text = formatDateTime(launch.net)
            findViewById<TextView>(R.id.mission_desc_text_view).text = launch.mission?.description
            findViewById<Button>(R.id.info_button).apply {
                text = "Info"
                visibility = if (launch.infoURLs == null) View.GONE else View.VISIBLE
                setOnClickListener {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(launch.infoURLs))
                    startActivity(browserIntent)
                }
            }
            findViewById<Button>(R.id.vid_button).apply {
                text = "Video"
                visibility = if (launch.vidURLs == null) View.GONE else View.VISIBLE
                setOnClickListener {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(launch.vidURLs))
                    startActivity(browserIntent)
                }
            }
        })
    }

    private fun setupCountdown(model: DetailsViewModel) {
        val textView = findViewById<TextView>(R.id.countdown_text_view)
        model.countdown.observe(this, Observer {
            textView.text = it
        })
    }

    private fun formatDateTime(dateTime: DateTime): String {
        return DateTimeFormat.forStyle("MM")
                .withLocale(Locale.getDefault())
                .print(dateTime.withZone(DateTimeZone.getDefault()))
    }
}