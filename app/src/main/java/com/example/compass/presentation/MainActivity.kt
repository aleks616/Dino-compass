package com.example.compass.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.example.compass.R
import com.example.compass.presentation.theme.CompassTheme

class MainActivity:ComponentActivity() {
    private var compass:Compass?=null
    private var arrowView:ImageView?=null
    private var sotwLabel:TextView?=null

    private var currentAzimuth=0f
    private val sotwFormatter:SOTWFormatter?=null

    private lateinit var locationManager:LocationManager
    private val locationPermissionLauncher=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions->
        if(permissions[Manifest.permission.ACCESS_FINE_LOCATION]==true) {
            setupCompass()
        }
    }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)

        assets.open("data").bufferedReader().forEachLine { line->
            val fields=Regex("""(\d+),"([^"]*)","([^"]*)","([^"]*)",Location\(([-\d.]+),([-\d.]+)\),"([^"]*)"""").matchEntire(line.trim())!!.groupValues

                dinoList.add(DinoLocation(fields[1].toInt(),fields[2],fields[3],fields[4],
                    Location(fields[5].toDouble(),fields[6].toDouble()),fields[7])
                )
        }

        setContent {
            WearApp(
                onViewsReady={handsView,labelView->
                    arrowView=handsView
                    sotwLabel=labelView
                }
            )
        }

        setupCompass()
    }

    override fun onStart() {
        super.onStart()
        compass?.start()
    }

    override fun onResume() {
        super.onResume()
        compass?.start()
    }

    override fun onPause() {
        compass?.stop()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        //Log.d(TAG,"stop compass")
        compass?.stop()
    }

    private fun setupCompass() {
        locationManager=getSystemService(LOCATION_SERVICE) as LocationManager

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000,
            1f
        ) {location->
            if(compass==null) {
                compass=Compass(this@MainActivity,location.latitude,location.longitude)
                compass?.setListener(getCompassListener())
                compass?.start()
            }
            else {
                compass?.setLocation(location.latitude,location.longitude)
            }
        }

        val lastLocation=locationManager.getLastKnownLocation(
            LocationManager.GPS_PROVIDER
        )

        if(lastLocation!=null) {
            compass=Compass(this,lastLocation.latitude,lastLocation.longitude)
            compass?.setListener(getCompassListener())
        }
    }

    private fun adjustArrow(azimuth:Float) {
        //Log.d(TAG,"will set rotation from $currentAzimuth to $azimuth")

        val animation=RotateAnimation(
            -currentAzimuth,
            -azimuth,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        ).apply {
            duration=500
            repeatCount=0
            fillAfter=true
        }

        currentAzimuth=azimuth
        arrowView?.startAnimation(animation)
    }

    private fun adjustSotwLabel(azimuth:Float) {
        sotwLabel?.text=sotwFormatter?.format(azimuth)?:""
    }

    private fun getCompassListener():Compass.CompassListener {
        return object:Compass.CompassListener {
            override fun onNewAzimuth(azimuth:Float) {
                runOnUiThread {
                    adjustArrow(azimuth)
                    adjustSotwLabel(azimuth)
                }
            }
        }
    }
}

@Composable
fun WearApp(
    onViewsReady:(ImageView,TextView)->Unit={_,_->}
) {
    CompassTheme {
        AppScaffold {
            val listState=rememberTransformingLazyColumnState()

            ScreenScaffold(
                scrollState=listState
            ) {_->
                Column(
                    modifier=Modifier
                        .fillMaxSize()
                        .padding(5.dp),
                    horizontalAlignment=Alignment.CenterHorizontally
                ) {
                    ListHeader(
                        modifier=Modifier
                            .fillMaxWidth()
                    ) {
                        val context=androidx.compose.ui.platform.LocalContext.current

                        val labelViewPlaceholder=remember {TextView(context).apply {
                            setTextColor(Color.WHITE)
                        }}
                        Column(
                            modifier=Modifier.fillMaxWidth(),
                        ) {
                            AndroidView(
                                factory={labelViewPlaceholder},
                                modifier=Modifier.align(Alignment.CenterHorizontally)
                            )
                            Box(
                                modifier=Modifier.fillMaxWidth(),
                                contentAlignment=Alignment.Center,

                            ) {
                                Image(
                                    painter=painterResource(R.drawable.dial2),
                                    contentDescription="Compass dial",
                                    modifier=Modifier.size(320.dp),
                                    contentScale=ContentScale.Fit
                                )
                                AndroidView(
                                    factory={viewContext->
                                        ImageView(viewContext).apply {
                                            setImageResource(R.drawable.hands2)
                                        }
                                    },
                                    modifier=Modifier
                                        .size(320.dp)
                                        .scale(0.85F),
                                    update={imageView->
                                        onViewsReady(
                                            imageView,
                                            labelViewPlaceholder
                                        )
                                    }
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}


@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    WearApp()
}
