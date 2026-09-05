package com.example.compass.presentation

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
data class Location(val latitude:Double,val longitude:Double)

class DinoLocation(id:Int,address:String,street:String,city:String,locationP:Location,zipCode:String) {
    var location:Location?=locationP

    fun getDistance(userLocation:Location):Double {
        val earthRadius=6371
        val deltaLat=(this.location!!.latitude-userLocation.latitude)*(Math.PI/180)
        val deltaLng=(this.location!!.latitude-userLocation.latitude)*(Math.PI/180)

        val a=sin(deltaLat/2)*sin(deltaLat/2)+
              cos(userLocation.latitude*(Math.PI/180))*cos(userLocation.latitude*(Math.PI/180))*
              sin(deltaLng/2)*sin(deltaLng/2)
        val centralAngle=2*atan2(sqrt(a),sqrt(1-a))
        return round(earthRadius*centralAngle)
    }
}


