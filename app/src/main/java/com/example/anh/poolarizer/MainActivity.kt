package com.example.anh.poolarizer

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.*
import com.google.android.gms.nearby.Nearby
import java.util.*
import android.widget.LinearLayout
import android.widget.NumberPicker
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.Strategy.P2P_CLUSTER
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import kotlin.text.Charsets.UTF_8


class MainActivity : AppCompatActivity() {

    // "Constants"
    private val MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 5
    private val STRATEGY = P2P_CLUSTER
    private val TAG = "ETM"
    private var ANDROID_ID = ""
    private val NUM_BALLS_PREFIX = "Number of Balls: "
    private val NUM_PLAYERS_PREFIX = "Number of Players: "
    private val FIND_PLAYERS = "Find Players"
    private val DEAL = "Deal"
    private val INITIAL_NUM_BALLS = 4

    // Game related variables
    private var numOfPlayers = 1
    private var numBallsPerPlayer = INITIAL_NUM_BALLS
    private var possibleNumbers = MutableList(15, {i -> (i+1).toString()})
    private var ballImages = Array(15, {i -> "ball" + (i+1).toString()})
    private var dealtNumbers = mutableListOf<String>()
    private var balls: List<String>? = null

    // Communication related variables
    private var connectionsClient: ConnectionsClient? = null
    private var opponentIDs = mutableSetOf<String>()

    // UI Variables
    private var findPlayersBtn: Button? = null
    private var dealBtn: Button? = null
    private var numPlayersView: TextView? = null
    private var numBallsView: TextView? = null
    private var numBallsPicker: NumberPicker? = null
    private var imageLayout: LinearLayout? = null
    private var weightSum = 6f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectionsClient = Nearby.getConnectionsClient(this)

        ANDROID_ID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        Log.i(TAG, ANDROID_ID)

        numBallsPicker = findViewById(R.id.numBallsPicker)
        numBallsPicker!!.minValue = 1
        numBallsPicker!!.maxValue = possibleNumbers.size / numOfPlayers
        numBallsPicker!!.value = INITIAL_NUM_BALLS
        numBallsPerPlayer = numBallsPicker!!.value

        numPlayersView = findViewById(R.id.numPlayersView)
        numPlayersView!!.text = NUM_PLAYERS_PREFIX + numOfPlayers.toString()

        numBallsView = findViewById(R.id.numBallsView)
        numBallsView!!.text = NUM_BALLS_PREFIX + numBallsPicker!!.value.toString()

        findPlayersBtn  = findViewById(R.id.findPlayersBtn)
        findPlayersBtn!!.text = FIND_PLAYERS

        dealBtn = findViewById(R.id.dealBtn)
        dealBtn!!.text = DEAL

        imageLayout = findViewById(R.id.imageLayout)
        imageLayout!!.weightSum = weightSum

        numBallsPicker!!.setOnValueChangedListener { picker, oldVal, newVal ->

            numBallsView!!.text = NUM_BALLS_PREFIX + newVal.toString()
            numBallsPerPlayer = newVal
        }

        findPlayersBtn!!.setOnClickListener {
            findPlayers()
        }

        dealBtn!!.setOnClickListener {
            dealNumbers()
        }

        disableDealerUI()
        checkForPermission()
    }

    override fun onStop() {
        connectionsClient!!.stopDiscovery()
        connectionsClient!!.stopAdvertising()
        super.onStop()
    }


    private fun findPlayers() {
        reset()
        startAdvertising()
        startDiscovery()
    }

    private fun reset() {
        connectionsClient!!.stopDiscovery()
        connectionsClient!!.stopAdvertising()
        opponentIDs.clear()
        imageLayout!!.removeAllViews()
        disableDealerUI()

        numOfPlayers = 1
        numBallsPerPlayer = INITIAL_NUM_BALLS
        dealtNumbers.clear()
        balls = null

        updateUI()
    }

    private fun updateUI() {
        numBallsPicker!!.maxValue = possibleNumbers.size / numOfPlayers
        numPlayersView!!.text = NUM_PLAYERS_PREFIX + numOfPlayers.toString()
        numBallsView!!.text = NUM_BALLS_PREFIX + numBallsPicker!!.value.toString()
    }

    private fun enableDealerUI() {
        dealBtn!!.isEnabled = true
        numBallsPicker!!.isEnabled = true
    }

    private fun disableDealerUI() {
        dealBtn!!.isEnabled = false
        numBallsPicker!!.isEnabled = false
    }

    private fun dealNumbers() {
        connectionsClient!!.stopDiscovery()
        connectionsClient!!.stopAdvertising()
        imageLayout!!.removeAllViews()
        generateNumbers()

        // Send out numbers to other players
        val listOfPlayerIDs = opponentIDs.toList()
        for (i in 0 until listOfPlayerIDs.size) {
            var dealtNumbersPerPlayer = ""
            for (j in 0 until numBallsPerPlayer) {
                dealtNumbersPerPlayer += dealtNumbers[i*listOfPlayerIDs.size + j]
                if (j != numBallsPerPlayer-1) {
                    dealtNumbersPerPlayer += ","
                }
            }

            sendPayload(dealtNumbersPerPlayer, listOfPlayerIDs[i])
        }

        // Set numbers for self
        balls = dealtNumbers.subList(numBallsPerPlayer * listOfPlayerIDs.size, dealtNumbers.size)
//        for (ball in balls!!) {
//            Log.i(TAG, "Self ball: " + ball)
//        }
        displayBalls(balls!!)
    }

    private fun displayBalls(balls: List<String>) {

        var intBalls = balls.map {ball -> ball.toInt()}

        var weight = 1f
        if (intBalls.size > 6) {
            weight = weightSum / intBalls.size
        }
        val layoutParams = LinearLayout.LayoutParams(0,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                        weight)

        for (ball in intBalls.sorted()) {
            var newBallImage = ImageView(this)
            val resID = resources.getIdentifier(ballImages[ball-1], "drawable", packageName)
            newBallImage.setImageResource(resID)
            newBallImage.layoutParams = layoutParams
            imageLayout!!.addView(newBallImage)
        }
    }

    private fun generateNumbers() {
        dealtNumbers.clear()

        possibleNumbers.shuffle()
        val numbersNeeded = numOfPlayers * numBallsPerPlayer
        dealtNumbers = possibleNumbers.slice((0 until numbersNeeded)).toMutableList()

//        for (i in 0 until dealtNumbers.size) {
//            Log.i("Dealt number: ", dealtNumbers[i])
//        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun startAdvertising() {
        connectionsClient!!.startAdvertising(
                ANDROID_ID,
                packageName,
                connectionLifecycleCallback,
                AdvertisingOptions(STRATEGY))
                .addOnSuccessListener(
                        OnSuccessListener<Void>() {
                            toast("Advertising Successfully")
                        })
                .addOnFailureListener(
                        OnFailureListener() {
                            toast("Advertising failed.")
                        })
    }

    private fun startDiscovery() {
        connectionsClient!!.startDiscovery(
                packageName,
                endpointDiscoveryCallBack,
                DiscoveryOptions(STRATEGY))
                .addOnSuccessListener(
                        OnSuccessListener<Void> {
                            toast("Discovering Successfully")
                        })
                .addOnFailureListener(
                        OnFailureListener {
                            toast("Discovering Failed.")
                        })
    }

    private val endpointDiscoveryCallBack = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(
                endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {

            Log.i(TAG, "onEndpointFound: endpoint found, connecting")
            toast("Endpoint found.")

            connectionsClient!!.requestConnection(ANDROID_ID, endpointId, connectionLifecycleCallback);
        }

        override fun onEndpointLost(endpointId: String) {
            Log.i(TAG, "onEndpointFound: endpoint connection lost")
            toast("Endpoint lost.")
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.i(TAG, "onConnectionInitiated: accepting connection")
            connectionsClient!!.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.i(TAG, "onConnectionResult: connection successful")
                toast("Connection Successful!")

                if (endpointId !in opponentIDs) {
                    opponentIDs.add(endpointId)
                    numOfPlayers++
                }
                updateUI()
                enableDealerUI()
            } else {
                Log.i(TAG, "onConnectionResult: connection failed")
                toast("Connection Failed!")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.i(TAG, "onDisconnected: disconnected from the opponent")
            toast("Disconnect!")
            if (endpointId in opponentIDs) {
                opponentIDs.remove(endpointId)
                numOfPlayers--
                if (opponentIDs.size == 0) {
                    disableDealerUI()
                }
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            connectionsClient!!.stopDiscovery()
            connectionsClient!!.stopAdvertising()

            val selectedNumbers = String(payload.asBytes()!!, UTF_8)
            balls = selectedNumbers.split(',').toMutableList()
            imageLayout!!.removeAllViews()
            displayBalls(balls!!)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                // Nothing to do here
            }
        }
    }

    private fun sendPayload(payLoad: String, opponentId: String) {
        val bytePayLoad = payLoad.map {x -> x.toByte()}.toByteArray()
        connectionsClient!!.sendPayload(opponentId, Payload.fromBytes(bytePayLoad))
    }

    private fun checkForPermission() {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    toast("Permission Granted!")
                } else {
                    toast("Permission Denied!")
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }
}
