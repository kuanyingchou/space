package com.ken.space.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bumptech.glide.request.RequestOptions
import com.google.accompanist.glide.GlideImage
import com.ken.space.SpaceTheme
import com.ken.space.model.DetailsViewModel
import com.ken.space.model.Launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.*


const val KEY_LAUNCH_ID = "LAUNCH_ID"

class DetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launchId = intent.getStringExtra(KEY_LAUNCH_ID)!!

        val model: DetailsViewModel by viewModel(
            parameters = { parametersOf(launchId) }
        )

        setContent {
            val launch = model.launch.collectAsState().value

            SpaceTheme.Scaffold(title = "Launch Details") {
                if(launch != null) {
                    Detail(launch, model)
                }
            }
        }
    }

    @Composable
    private fun Detail(launch: Launch, model: DetailsViewModel) {
        val countdown = model.countdown.collectAsState("").value

        Column() {
            GlideImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1F),
                data = launch.image ?: "",
                contentDescription = "",
                fadeIn = true,
                requestBuilder = {
                    val options = RequestOptions()
                    options.centerCrop()
                    apply(options)
                }
            )
            Column(modifier = Modifier.padding(8.dp, 8.dp)) {
                Text(text = launch.name, style = MaterialTheme.typography.h5)
                Text(text = formatDateTime(launch.net), style = MaterialTheme.typography.caption)
                Text(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = countdown,
                    style = MaterialTheme.typography.h4)
                Text(text = launch.mission?.description ?: "", style = MaterialTheme.typography.body1)

                launch.infoURLs?.let {
                    Button(onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                        startActivity(browserIntent)
                    }) {}
                }
            }
        }
    }

    private fun formatDateTime(dateTime: DateTime): String {
        return DateTimeFormat.forStyle("MM")
                .withLocale(Locale.getDefault())
                .print(dateTime.withZone(DateTimeZone.getDefault()))
    }
}