package org.mightyfrog.android.recaptchasample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.safetynet.SafetyNet
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import rx.Single
import rx.SingleSubscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * https://developer.android.com/training/safetynet/recaptcha.html
 *
 * @author Shigehiro Soejima
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "reCAPTCHA Sample"
        private const val baseUrl = "https://www.google.com/"
        private const val siteKey = "YOUR SITE KEY"
        private const val secretKey = "YOUR SECRET KEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener {
            sendVerifyRequest()
        }
    }

    private fun sendVerifyRequest() {
        SafetyNet.getClient(this).verifyWithRecaptcha(siteKey)
                .addOnSuccessListener(this) { response ->
                    if (!response.tokenResult.isEmpty()) {
                        handleSiteVerify(response.tokenResult)
                    }
                }
                .addOnFailureListener(this) { e ->
                    if (e is ApiException) {
                        Log.e(TAG, CommonStatusCodes.getStatusCodeString(e.statusCode))
                    } else {
                        Log.e(TAG, e.message)
                    }
                }
    }

    private fun handleSiteVerify(token: String) {
        val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        retrofit.create(reCAPTCHAApi::class.java).verify(secretKey, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleSubscriber<JsonObject>() {
                    override fun onSuccess(t: JsonObject?) {
                        t?.apply {
                            textView.text = toString()
                        }
                    }

                    override fun onError(error: Throwable?) {
                        error?.apply {
                            textView.text = toString()
                        }
                    }
                })
    }

    internal interface reCAPTCHAApi {
        @FormUrlEncoded
        @POST("recaptcha/api/siteverify")
        fun verify(@Field("secret") secret: String, @Field("response") response: String): Single<JsonObject>
    }
}
