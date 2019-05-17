package com.github.repos

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    val GITHUB_URL = "https://github.com/login/oauth/authorize"
    val GITHUB_OAUTH = "https://github.com/login/oauth/access_token"
    var CODE = ""
    var PACKAGE = ""
    var CLIENT_ID = "e35bed254c8d3961ff23"
    var CLIENT_SECRET = "a4dd3bb6e266a218d57b0bd4786e3e374e384d42"

    private val TAG = "github-oauth"

    var scopeAppendToUrl = ""
    lateinit var scopeList: List<String>

    private val clearDataBeforeLaunch = false
    private val isScopeDefined = false
    private val debug = false
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scopeList = ArrayList()
        scopeAppendToUrl = ""

        val intent = intent


        var urlLoad = "$GITHUB_URL?client_id=e35bed254c8d3961ff23"

        if (isScopeDefined) {
            scopeList = intent.getStringArrayListExtra("scope_list")
            scopeAppendToUrl = getCsvFromList(scopeList)
            urlLoad += "&scope=$scopeAppendToUrl"
        }


        if (clearDataBeforeLaunch) {
            clearDataBeforeLaunch()
        }




        webview.settings.javaScriptEnabled = true
        webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                super.shouldOverrideUrlLoading(view, request)
                try {
                    val url = request!!.url.toString()
                    if (url.contains("?code=")) return false
                    CODE = url.substring(url.lastIndexOf("?code=") + 1)
                    val tokenCode = CODE.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val tokenFetchedIs = tokenCode[1]
                    val cleanToken =
                        tokenFetchedIs.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    fetchOauthTokenWithCode(cleanToken[0])

                } catch (e: NullPointerException) {
                    e.printStackTrace()
                } catch (e: ArrayIndexOutOfBoundsException) {
                    e.printStackTrace()
                }
                return true
            }

        }

        webview!!.loadUrl(urlLoad)
    }

    private fun clearDataBeforeLaunch() {
        val cookieManager = CookieManager.getInstance()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies { aBoolean ->
                // a callback which is executed when the cookies have been removed
                Log.d(TAG, "Cookie removed: " + aBoolean!!)
            }
        } else {

            cookieManager.removeAllCookies {  }
        }
    }

    private fun fetchOauthTokenWithCode(code: String) {
        val client = OkHttpClient()
        val url = HttpUrl.parse(GITHUB_OAUTH)!!.newBuilder()
        url.addQueryParameter("client_id", CLIENT_ID)
        url.addQueryParameter("client_secret", CLIENT_SECRET)
        url.addQueryParameter("code", code)

        val url_oauth = url.build().toString()

        val request = Request.Builder()
            .header("Accept", "application/json")
            .url(url_oauth)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (debug) {
                    Log.d(TAG, "IOException: " + e.message)
                }

                finishThisActivity(ResultCode.ERROR)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = response.body()!!.string()

                    try {
                        val jsonObject = JSONObject(json)
                        val authToken = jsonObject.getString("access_token")

                        storeToSharedPreference(authToken)

                        if (debug) {
                            Log.d(TAG, "token is: $authToken")
                        }

                    } catch (exp: JSONException) {
                        if (debug) {
                            Log.d(TAG, "json exception: " + exp.message)
                        }
                    }

                } else {
                    if (debug) {
                        Log.d(TAG, "onResponse: not success: " + response.message())
                    }
                }

                finishThisActivity(ResultCode.SUCCESS)
            }
        })

    }

    // Allow web view to go back a page.
    override fun onBackPressed() {
        if (webview!!.canGoBack()) {
            webview!!.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun storeToSharedPreference(auth_token: String) {
        val prefs = getSharedPreferences("github_prefs", MODE_PRIVATE)
        val edit = prefs.edit()

        edit.putString("oauth_token", auth_token)
        edit.apply()
    }


    private fun finishThisActivity(resultCode: Int) {
        setResult(resultCode)
        finish()
    }

    private fun getCsvFromList(scopeList: List<String>): String {
        var csvString = ""

        for (scope in scopeList) {
            if (csvString != "") {
                csvString += ","
            }

            csvString += scope
        }

        return csvString
    }

    object ResultCode {
        const val SUCCESS = 1
        const val ERROR = 2
    }

}
