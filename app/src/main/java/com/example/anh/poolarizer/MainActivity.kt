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
import com.google.android.gms.nearby.messages.Message
import com.google.android.gms.nearby.messages.MessageListener
import com.google.android.gms.nearby.messages.MessagesClient
import java.lang.System.currentTimeMillis
import java.util.*
import java.util.UUID.randomUUID
import android.widget.LinearLayout
import android.widget.NumberPicker
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.Strategy.P2P_CLUSTER
import com.google.android.gms.nearby.messages.Strategy
import com.google.android.gms.nearby.messages.SubscribeOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import kotlin.text.Charsets.UTF_8


class MainActivity : AppCompatActivity() {

    ////////////////////////////////////// Nearby Connections

    val MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 5
    val STRATEGY = P2P_CLUSTER
    var connectionsClient: ConnectionsClient? = null

    /////////////////////////////////////

    // "Constants"
    private val TAG = "ETM"
    private val UUID = randomUUID().toString()
    private var ANDROID_ID = ""
//    private val ARBITRATION = "ARB"
//    private val DEALING = "DEAL"
//    private val STATUS = "STATUS"
//    private val LEAVE = "Leave"
    private val FIND_PLAYERS = "Find Players"
    private val DEAL = "Deal"

    // Game related variables
    private var numOfPlayers = 1
    private var numBallsPerPlayer = 2
    private var possibleNumbers = Array(15, {i -> (i+1).toString()})
    private var ballImages = Array(15, {i -> "ball" + (i+1).toString()})
    private var dealtNumbers = mutableListOf<String>()
    private var balls: List<String>? = null
//    private var dealingMode = false

    // Communication related variables
//    private var messageListener: MessageListener? = null
//    private var messageClient: MessagesClient? = null
//    private var messages = mutableListOf<Message>()
//    private var selfTimestamp = ""
//    private var playerIDs = mutableMapOf<String, String>()
    private var opponentIDs = mutableSetOf<String>()
//    private val strategy = Strategy.Builder()
//            .setDiscoveryMode(Strategy.DISCOVERY_MODE_DEFAULT)
//            .setDistanceType(Strategy.DISTANCE_TYPE_DEFAULT)
//            .setTtlSeconds(Strategy.TTL_SECONDS_DEFAULT)
//            .build()
//    private val options = SubscribeOptions.Builder()
//            .setStrategy(strategy)
//            .build()

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

        ANDROID_ID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        Log.i(TAG, ANDROID_ID)

        numBallsPicker = findViewById(R.id.numBallsPicker)
        numBallsPicker!!.minValue = 1
        numBallsPicker!!.maxValue = possibleNumbers.size / numOfPlayers
        numBallsPicker!!.value = 2
        numBallsPerPlayer = numBallsPicker!!.value

        numPlayersView = findViewById(R.id.numPlayersView)
        numPlayersView!!.text = "Number of Players: " + numOfPlayers.toString()

        numBallsView = findViewById(R.id.numBallsView)
        numBallsView!!.text = "Number of Balls: " + numBallsPicker!!.value.toString()

        findPlayersBtn  = findViewById(R.id.findPlayersBtn)
        findPlayersBtn!!.text = FIND_PLAYERS

        dealBtn = findViewById(R.id.dealBtn)
        dealBtn!!.text = DEAL

        imageLayout = findViewById(R.id.imageLayout)
        imageLayout!!.weightSum = weightSum

        numBallsPicker!!.setOnValueChangedListener { picker, oldVal, newVal ->

            numBallsView!!.text = "Number of Balls: " + newVal.toString()
            numBallsPerPlayer = newVal
        }

        findPlayersBtn!!.setOnClickListener {
//            if (findPlayersBtn!!.text == FIND_PLAYERS) {
                findPlayers()
//            } else if (findPlayersBtn!!.text == LEAVE) {
//                reset()
//            }
        }

        dealBtn!!.setOnClickListener {
            dealNumbers()
        }

//        messageClient = Nearby.getMessagesClient(this)
//        this.messageListener = object: MessageListener() {
//
//            override fun onFound(message: Message) {
//                Log.i(TAG, "Found message: " + String(message.content))
//                toast("Found: " + String(message.content))
//                parseMessage(message)
//            }
//
//            override fun onLost(message: Message) {
//                Log.i(TAG, "Lost sight of message: " + String(message.content))
//                toast("Lost: " + String(message.content))
//            }
//        }


//        messageClient!!.subscribe(messageListener!!, options)
        disableDealerUI()
        checkForPermission()
    }

    override fun onStop() {
        connectionsClient!!.stopDiscovery()
        connectionsClient!!.stopAdvertising()
//        unpublishMessages()
//        messageClient!!.unsubscribe(messageListener!!)
        super.onStop()
    }


    private fun findPlayers() {
        /////////////////////// Nearby Connections
        connectionsClient = Nearby.getConnectionsClient(this)
        connectionsClient!!.stopAdvertising()
        startAdvertising()
        connectionsClient!!.stopDiscovery()
        startDiscovery()
        /////////////////////////////////////////
//        selfTimestamp = currentTimeMillis().toString().drop(7)
//        val byteArrayMessage = "$ARBITRATION:$ANDROID_ID:$selfTimestamp".toByteArray()
//        messages.add(Message(byteArrayMessage))
//        messageClient!!.publish(messages.last())
//        Log.i(TAG, selfTimestamp)
//        findPlayersBtn!!.text = LEAVE
    }

    private fun reset() {
        connectionsClient!!.stopDiscovery()
        connectionsClient!!.stopAdvertising()
//        unpublishMessages()
        imageLayout!!.removeAllViews()
//        messageClient!!.unsubscribe(messageListener!!)
//        messageClient!!.subscribe(messageListener!!, options)
//        dealingMode = false
        disableDealerUI()

        numOfPlayers = 1
        numBallsPerPlayer = 2
        findPlayersBtn!!.text = FIND_PLAYERS
        dealtNumbers.clear()
        balls = null
//        selfTimestamp = ""
//        playerIDs.clear()

        numPlayersView!!.text = "Number of Players: " + numOfPlayers.toString()
        numBallsPicker!!.value = numBallsPerPlayer
        numBallsView!!.text = "Number of Balls: " + numBallsPicker!!.value.toString()
    }

//    private fun unpublishMessages() {
//        messages.forEach {message -> messageClient!!.unpublish(message)}
//    }

    private fun updateUI() {
        numBallsPicker!!.maxValue = possibleNumbers.size / numOfPlayers
        numPlayersView!!.text = "Number of players: " + numOfPlayers.toString()
        numBallsView!!.text = "Number of Balls: " + numBallsPicker!!.value.toString()
        for (id in opponentIDs) {
            Log.i(TAG, "Opponent ID: $id")
        }
    }

//    private fun parseMessage(message: Message) {
//        var content = String(message.content).split(':')
//        if (content[0] == ARBITRATION) {
//             Arbitration phase.
//            if (content[1] !in playerIDs) {
//                playerIDs[content[1]] = content[2]
//                numOfPlayers++
//                updateUI()
//            }
//
//            if (selfTimestamp != "") {
//                arbitrate()
//            }
//
//        } else if (content[0] == DEALING) {
//            if ((content[1] == ANDROID_ID) && !dealingMode) {
//                balls = content[2].split(',').toMutableList()
//                imageLayout!!.removeAllViews()
//                displayBalls(balls!!)
//                deleteMessages(ANDROID_ID)
//                notifyDealer(content[1])
//            }
//
//        } else if (content[0] == STATUS) {
//            deleteMessages(content[1])
//            deleteMessages(ANDROID_ID)
//        }
//    }

//    private fun arbitrate() {
//        var win = true
//
//        for ((_, timestamp) in playerIDs) {
//            if (timestamp.toInt() < selfTimestamp.toInt()) {
//                win = false
//                break
//            }
//        }
//
//        if (win) {
//            dealingMode = true
//            enableDealerUI()
//        } else {
//            dealingMode = false
//            disableDealerUI()
//        }
//    }

//    private fun notifyDealer(stringMsg: String) {
//        val byteArrayMessage = "$STATUS:stringMsg".toByteArray()
//        messages.add(Message(byteArrayMessage))
//        messageClient!!.publish(messages.last())
//    }
//
//    private fun deleteMessages(playerID: String) {
//        for (message in messages) {
//            if (playerID in message.content.toString()) {
//                messageClient!!.unpublish(message)
//            }
//        }
//    }

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
//        val listOfPlayerIDs = playerIDs.keys.toList()
//        for (i in 0 until listOfPlayerIDs.size) {
//            var dealtNumbersPerPlayer = ""
//            for (j in 0 until numBallsPerPlayer) {
//                dealtNumbersPerPlayer += dealtNumbers[i*listOfPlayerIDs.size + j]
//                if (j != numBallsPerPlayer-1) {
//                    dealtNumbersPerPlayer += ","
//                }
//            }
//            val byteArrayMessage = "$DEALING:${listOfPlayerIDs[i]}:$dealtNumbersPerPlayer".toByteArray()
//            messages.add(Message(byteArrayMessage))
//            messageClient!!.publish(messages.last())


//        }

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
        for (ball in balls!!) {
            Log.i(TAG, "Self ball: " + ball)
        }
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

        var totalNumbers = possibleNumbers.size
        // Add 1 for self
        val numbersNeeded = numOfPlayers * numBallsPerPlayer
        fun ClosedRange<Int>.random() = Random().nextInt(endInclusive - start) +  start
        var randomIndex: Int

        for (i in 0 until numbersNeeded) {
            randomIndex = (0..totalNumbers).random()
            Log.d("Random Index: ", randomIndex.toString())
            dealtNumbers.add(possibleNumbers[randomIndex])
            swapNumbers(randomIndex, totalNumbers-1)
            totalNumbers--
        }

        for (i in 0 until dealtNumbers.size) {
            Log.i("Dealt number: ", dealtNumbers[i])
        }
    }

    private fun swapNumbers(i: Int, j: Int) {
        val temp = possibleNumbers[i]
        possibleNumbers[i] = possibleNumbers[j]
        possibleNumbers[j] = temp
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    ////////////////////////////////////////////// Nearby Connections
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
//                connectionsClient!!.stopDiscovery()
//                connectionsClient!!.stopAdvertising()
                if (endpointId !in opponentIDs) {
                    opponentIDs.add(endpointId)
                }
                numOfPlayers++
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
