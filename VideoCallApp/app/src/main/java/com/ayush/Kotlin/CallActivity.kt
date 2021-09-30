package com.ayush.Kotlin

import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*

class CallActivity : AppCompatActivity() {

    lateinit var webView:WebView;
    lateinit var callLayout:LinearLayout;
    lateinit var incomingCallTxt:TextView;
    lateinit var acceptBtn:ImageView;
    lateinit var rejectBtn:ImageView;
    lateinit var inputLayout:LinearLayout;
    lateinit var callControlLayout:LinearLayout;
    var username = "";
    var freindusername = ""

    var isPeerConnected = false;

    var firebaseRef = Firebase.database.getReference("users")

    var isAudio = true
    var isVideo = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        webView = (findViewById<WebView>(R.id.webView))
        callLayout = findViewById(R.id.incomingCallLayout)
        acceptBtn = findViewById(R.id.imageViewCallUp)
        rejectBtn = findViewById(R.id.imageViewCallDown)
        inputLayout = findViewById(R.id.callPersonLayout)
        callControlLayout = findViewById(R.id.toggleGrpLayout)


        var callBtn = findViewById<Button>(R.id.callBtn)
        incomingCallTxt = findViewById(R.id.incomingCallTextView)
        var toggleAudioBtn = findViewById<ImageView>(R.id.audioToggleBtn)
        var toggleVideoBtn = findViewById<ImageView>(R.id.videoToggleBtn)
        var friendNameEdit = findViewById<EditText>(R.id.editTextCallOther)

        username = intent.getStringExtra("username")!!

        callBtn.setOnClickListener {
            freindusername = friendNameEdit.text.toString()
            sendCallRequest()
        }

        toggleAudioBtn.setOnClickListener{
            isAudio = !isAudio
            callJavascriptFunction("javascript:toggleAudio(\"${isAudio}\")")
            toggleAudioBtn.setImageResource(if(isAudio) R.drawable.ic_baseline_mic_24 else R.drawable.ic_baseline_mic_off_24)
        }

        toggleVideoBtn.setOnClickListener{
            isVideo = !isVideo
            callJavascriptFunction("javascript:toggleAudio(\"${isVideo}\")")
            toggleVideoBtn.setImageResource(if(isVideo) R.drawable.ic_baseline_videocam_24 else R.drawable.ic_baseline_videocam_off_24)
        }



        setupWebView()
    }

    private fun sendCallRequest() {
        if (!isPeerConnected){
            Toast.makeText(this,"You are not connected, check your internet",Toast.LENGTH_LONG).show()
            return
        }

        firebaseRef.child(freindusername).child("incoming").setValue(username)
        firebaseRef.child(freindusername).child("isAvailable").addValueEventListener(object:ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.value.toString() == "true"){
                    listenForConnId()
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun listenForConnId() {
        firebaseRef.child(freindusername).child("connId").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.value ==null){
                    return
                }
                switchToControls()
                callJavascriptFunction("javascript:startCall(\"${snapshot.value}\")")
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    public fun setupWebView(){
        webView.webChromeClient = object: WebChromeClient(){
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }
        webView.settings.javaScriptEnabled = true;
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(JavascriptInterface(this), "Android")

        loadVideoCall()
    }

    private fun loadVideoCall() {
        val filePath="file:android_asset/call.html"
        webView.loadUrl(filePath)

        webView.webViewClient = object:WebViewClient(){
            override fun onPageFinished(view: WebView?, url: String?) {
                initializePeer()
            }
        }
    }

    private var uniqueId: String = ""

    private fun initializePeer() {

        uniqueId = getUniqueId()

        callJavascriptFunction("javascript:init(\"${uniqueId}\")")
        firebaseRef.child(username).child("incoming").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                onCallRequest(snapshot.value as? String)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun onCallRequest(caller: String?) {
        if(caller == null) return
        callLayout.visibility = View.VISIBLE
        incomingCallTxt.text = "$caller is calling ..."

        acceptBtn.setOnClickListener{
            firebaseRef.child(username).child("connId").setValue(uniqueId)
            firebaseRef.child(username).child("isAvailable").setValue(true)

            callLayout.visibility = View.GONE
            switchToControls()
        }
        rejectBtn.setOnClickListener{
            firebaseRef.child(username).child("incoming").setValue(null)
            callLayout.visibility = View.GONE
        }
    }

    private fun switchToControls() {
        inputLayout.visibility = View.GONE
        callControlLayout.visibility = View.VISIBLE
    }

    private fun getUniqueId():String{
        return UUID.randomUUID().toString()
    }

    private fun callJavascriptFunction(functionstring:String){
        webView.post {
            webView.evaluateJavascript(functionstring, fun (a){
                Toast.makeText(applicationContext,a,Toast.LENGTH_LONG).show();
            })

        }
    }

    fun onPeerConnected() {
        isPeerConnected = true
    }

    override fun onBackPressed(){
        finish()
    }

    override fun onDestroy() {
        firebaseRef.child(username).setValue(null)
        webView.loadUrl("about:blank")
        super.onDestroy()
    }
}