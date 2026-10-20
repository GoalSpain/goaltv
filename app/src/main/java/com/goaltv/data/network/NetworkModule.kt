package com.goaltv.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Dns
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Centralized network module providing standard OkHttpClient instances using Dns.SYSTEM.
 */
object NetworkModule {

    private val _isDohEnabled = MutableStateFlow(false)
    val isDohEnabled: StateFlow<Boolean> = _isDohEnabled.asStateFlow()

    fun setDohEnabled(enabled: Boolean) {
        _isDohEnabled.value = enabled
    }

    fun getOkHttpClient(dohEnabled: Boolean = false): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .dns(Dns.SYSTEM)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "IPTVSmartersPro")
                    .header("Accept", "*/*")
                    .header("Connection", "keep-alive")
                    .build()
                chain.proceed(request)
            }

        if (ProtonVpnManager.isVpnActive.value) {
            builder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "IPTVSmartersPro/1.0 (WireGuard-NL254)")
                    .header("X-Forwarded-For", ProtonVpnManager.PROTON_GATEWAY_IP)
                    .header("X-Real-IP", ProtonVpnManager.PROTON_GATEWAY_IP)
                    .header("Accept", "*/*")
                    .header("Connection", "keep-alive")
                    .build()
                chain.proceed(request)
            }
        }

        return builder.build()
    }

    fun getPlayerOkHttpClient(dohEnabled: Boolean = false): OkHttpClient {
        return getOkHttpClient(false)
    }

    fun createXtreamCodesService(dohEnabled: Boolean = false): XtreamCodesService {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val okHttpClient = getOkHttpClient(false)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://zeta.proxy-pass.xyz/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(XtreamCodesService::class.java)
    }
}