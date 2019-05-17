package com.github.repos

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_login.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [LoginFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class LoginFragment : Fragment() {
    val GITHUB_URL = "https://github.com/login/oauth/authorize"
    val GITHUB_OAUTH = "https://github.com/login/oauth/access_token"
    var CODE = ""
    var PACKAGE = ""
    var CLIENT_ID = "ddffc694744e748cd9b9"
    var CLIENT_SECRET = "a50ac3c4e6a89f7c92c38e4b97f03a974dc5da9c"

    private val TAG = "github-oauth"
    private var progDailog: ProgressDialog? = null

    var scopeAppendToUrl = ""
    lateinit var scopeList: List<String>

    private val clearDataBeforeLaunch = false
    private val isScopeDefined = false
    private val debug = false
    private var navigation: NavController? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progDailog = ProgressDialog.show(activity, "Loading", "Please wait...", true)
        progDailog!!.setCancelable(false)
        navigation = Navigation.findNavController(view)
        scopeList = ArrayList()
        scopeAppendToUrl = ""
        //val intent = intent


        // Enable Javascript
        val webSettings = webview.settings
        webSettings.javaScriptEnabled = true

        // Force links and redirects to open in the WebView instead of in a browser
        val urlLoad = "$GITHUB_URL?client_id=e35bed254c8d3961ff23"

        webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                super.shouldOverrideUrlLoading(view, request)
                try {
                    val url = request!!.url.toString()
                    webview.loadUrl(url)

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

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.d(TAG, "Errorr on loading: " + error!!.description!!.toString())
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                super.onReceivedSslError(view, handler, error)
                handler!!.proceed()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progDailog!!.dismiss()
            }


        }

        webview.loadUrl("www.google.com")
    }

    private fun clearDataBeforeLaunch() {
        val cookieManager = CookieManager.getInstance()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies { aBoolean ->
                // a callback which is executed when the cookies have been removed
                Log.d(TAG, "Cookie removed: " + aBoolean!!)
            }
        } else {

            cookieManager.removeAllCookies { }
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

                // finishThisActivity(ResultCode.ERROR)
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
            }
        })

    }


    private fun storeToSharedPreference(auth_token: String) {
        //     val prefs = getSharedPreferences("github_prefs", AppCompatActivity.MODE_PRIVATE)
        //   val edit = prefs.edit()

        // edit.putString("oauth_token", auth_token)
        //  edit.apply()
        val directions = LoginFragmentDirections.actionLoginFragmentToRepoFragment(auth_token)
        navigation!!.navigate(directions)
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
