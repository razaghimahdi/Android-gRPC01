package com.example.grpc01

import android.app.Activity
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var channel: ManagedChannel

    /**
    Run the app on the emulator
    For the host use 10.0.2.2
    For the port use 50051
    Then add a message
    Click Submit
    You should get back Hello and what ever you placed in the message component and see it in the result component
     * */

    private lateinit var hostEdit: EditText
    private lateinit var portEdit: EditText
    private lateinit var messageEdit: EditText
    private lateinit var sendButton: Button
    private lateinit var resultText: TextView

    private val TAG = "AppDebug MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        hostEdit = findViewById(R.id.host);
        portEdit = findViewById(R.id.port);
        messageEdit = findViewById(R.id.message);
        sendButton = findViewById(R.id.send);
        resultText = findViewById(R.id.result);
        resultText.movementMethod = ScrollingMovementMethod();

        sendButton.setOnClickListener {
            sendGrpcMessage()
        }

    }


    private fun sendGrpcMessage() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(hostEdit.windowToken, 0)
        sendButton.isEnabled = false
        resultText.text = ""
        GrpcTask(this)
            .execute(
                hostEdit.text.toString(),
                messageEdit.text.toString(),
                portEdit.text.toString()
            )
    }

    inner class GrpcTask(activity: Activity) :
        AsyncTask<String?, Void?, String>() {
        private val activityReference: WeakReference<Activity> = WeakReference(activity)
        private lateinit var channel: ManagedChannel
        override fun doInBackground(vararg params: String?): String? {
            val host = params[0]
            val message = params[1]
            val portStr = params[2]
            val port = if (TextUtils.isEmpty(portStr)) 0 else Integer.valueOf(portStr)
            Log.i(TAG, "doInBackground host: " + host)
            Log.i(TAG, "doInBackground message: " + message)
            Log.i(TAG, "doInBackground portStr: " + portStr)
            Log.i(TAG, "doInBackground port: " + port)
            return try {
                channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
                val stub: GreeterGrpc.GreeterBlockingStub = GreeterGrpc.newBlockingStub(channel)
                val request: HelloRequest = HelloRequest.newBuilder().setName(message).build()
                val reply: HelloReply = stub.sayHello(request)
                Log.i(TAG, "doInBackground reply: " + reply.toString())
                reply.message
            } catch (e: Exception) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                e.printStackTrace(pw)
                pw.flush()
                Log.i(TAG, "doInBackground e: " + e.message)
                String.format("Failed... : %n%s", sw)
            }
        }

        override fun onPostExecute(result: String) {

            try {
                channel!!.shutdown().awaitTermination(1, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            Log.i(TAG, "onPostExecute result: " + result)
            val activity = activityReference.get() ?: return
            val resultText = activity.findViewById<View>(R.id.result) as TextView
            val sendButton = activity.findViewById<View>(R.id.send) as Button
            resultText.text = result
            sendButton.isEnabled = true
        }

    }


}