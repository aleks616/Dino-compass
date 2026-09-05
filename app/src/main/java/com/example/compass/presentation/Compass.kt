package com.example.compass.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


class Compass(context:Context,latitude:Double,longitude:Double):SensorEventListener {
    interface CompassListener {
        fun onNewAzimuth(azimuth:Float)
    }

    private var listener:CompassListener?=null

    private val sensorManager:SensorManager=context
        .getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gsensor:Sensor?=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val msensor:Sensor?=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val mGravity=FloatArray(3)
    private val mGeomagnetic=FloatArray(3)
    private val R=FloatArray(9)
    private val I=FloatArray(9)

    private var azimuth=0f
    private var azimuthFix=0f
    private var targetDirection=0f

    init {
        setLocation(latitude,longitude)
    }

    fun start() {
        sensorManager.registerListener(
            this,gsensor,
            SensorManager.SENSOR_DELAY_GAME
        )
        sensorManager.registerListener(
            this,msensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun setLocation(latitude:Double,longitude:Double) {
        val userLocation=Location(latitude,longitude)
        val nearestStore=dinoList.minByOrNull {
            it.getDistance(userLocation)
        }

        if(nearestStore!=null) {
            val storeLocation=nearestStore.location!!

            val userLatitude=Math.toRadians(latitude)
            val storeLatitude=Math.toRadians(storeLocation.latitude)
            val deltaLongitude=Math.toRadians(storeLocation.longitude-longitude)

            val y=sin(deltaLongitude)*cos(storeLatitude)
            val x=cos(userLatitude)*sin(storeLatitude)-sin(userLatitude)*cos(storeLatitude)*cos(deltaLongitude)

            targetDirection=((Math.toDegrees(atan2(y,x))+360)%360).toFloat()
        }
    }

    fun setAzimuthFix(fix:Float) {
        azimuthFix=fix
    }

    /*fun resetAzimuthFix() {
        setAzimuthFix(0f)
    }*/

    fun setListener(l:CompassListener?) {
        listener=l
    }

    override fun onSensorChanged(event:SensorEvent) {
        val alpha=0.97f

        synchronized(this) {
            if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER) {
                mGravity[0]=(alpha*mGravity[0]+(1-alpha)
                             *event.values[0])
                mGravity[1]=(alpha*mGravity[1]+(1-alpha)
                             *event.values[1])
                mGravity[2]=(alpha*mGravity[2]+(1-alpha)
                             *event.values[2])

                // mGravity = event.values;

                // Log.e(TAG, Float.toString(mGravity[0]));
            }
            if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD) {
                // mGeomagnetic = event.values;

                mGeomagnetic[0]=(alpha*mGeomagnetic[0]+(1-alpha)
                                 *event.values[0])
                mGeomagnetic[1]=(alpha*mGeomagnetic[1]+(1-alpha)
                                 *event.values[1])
                mGeomagnetic[2]=(alpha*mGeomagnetic[2]+(1-alpha)
                                 *event.values[2])

                // Log.e(TAG, Float.toString(event.values[0]));
            }

            val success=SensorManager.getRotationMatrix(
                R,I,mGravity,
                mGeomagnetic
            )
            if(success) {
                val orientation:FloatArray?=FloatArray(3)
                SensorManager.getOrientation(R,orientation)
                // Log.d(TAG, "azimuth (rad): " + azimuth);
                azimuth=Math.toDegrees(orientation!![0].toDouble()).toFloat() // orientation
                azimuth=(azimuth+azimuthFix+360)%360
                azimuth=(targetDirection-azimuth+360)%360
                // Log.d(TAG, "azimuth (deg): " + azimuth);
                if(listener!=null) {
                    listener!!.onNewAzimuth(azimuth)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int) {
    }

    /*companion object {
        private const val TAG="Compass"
    }*/
}