package com.ucl.energygrid.data.remote.apis

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://immersive-analytics-for-energy-grid-data.uksouth.cloudapp.azure.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }

    val pinApi: PinApi by lazy {
        retrofit.create(PinApi::class.java)
    }

    val mineApi: MineApi by lazy {
        retrofit.create(MineApi::class.java)
    }

    val energyApi: EnergyApi by lazy {
        retrofit.create(EnergyApi::class.java)
    }

    val siteApi: SiteApi by lazy {
        retrofit.create(SiteApi::class.java)
    }
}