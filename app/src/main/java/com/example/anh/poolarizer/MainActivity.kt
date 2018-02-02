package com.example.anh.poolarizer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
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


class MainActivity : AppCompatActivity() {

    // "Constants"
    private val TAG = "ETM"
    private val UUID = randomUUID().toString()
//    private var ANDROID_ID = ""
    private val ARBITRATION = "ARBITRATION"
    private val DEALING = "DEALING"
    private val LEAVE = "Leave"
    private val FIND_PLAYERS = "Find Players"
    private val DEAL = "Deal"

    // Game related variables
    private var numOfPlayers = 1
    private var numBallsPerPlayer = 2
    private var possibleNumbers = Array(15, {i -> (i+1).toString()})
    private var ballImages = Array(15, {i -> "ball" + (i+1).toString()})
    private var dealtNumbers = mutableListOf<String>()
    private var balls: List<String>? = null
    private var dealingMode = false

    // Communication related variables
    private var messageListener: MessageListener? = null
    private var messageClient: MessagesClient? = null
    private var messages = mutableListOf<Message>()
    private var selfTimestamp = ""
    private var playerIDs = mutableMapOf<String, String>()

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

//        ANDROID_ID = Secure.getString(contentResolver, Secure.ANDROID_ID)

        Log.i(TAG, UUID)

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
            if (findPlayersBtn!!.text == FIND_PLAYERS) {
                findPlayers()
            } else if (findPlayersBtn!!.text == LEAVE) {
                reset()
            }
        }

        dealBtn!!.setOnClickListener {
            dealNumbers()
        }

        messageClient = Nearby.getMessagesClient(this)
        this.messageListener = object: MessageListener() {

            override fun onFound(message: Message) {
                Log.i(TAG, "Found message: " + String(message.content))
                toast("Found: " + String(message.content))
                parseMessage(message)
            }

            override fun onLost(message: Message) {
                Log.i(TAG, "Lost sight of message: " + String(message.content))
                toast("Lost: " + String(message.content))
            }
        }

        messageClient!!.subscribe(messageListener!!)
        disableDealerUI()
    }

    override fun onStop() {
        unpublishMessages()
        messageClient!!.unsubscribe(messageListener!!)
        super.onStop()
    }

    private fun reset() {
        unpublishMessages()
        imageLayout!!.removeAllViews()
        dealingMode = false
        disableDealerUI()

        numOfPlayers = 1
        numBallsPerPlayer = 2
        findPlayersBtn!!.text = FIND_PLAYERS
        dealtNumbers.clear()
        balls = null
        selfTimestamp = ""
        playerIDs.clear()

        numPlayersView!!.text = "Number of Players: " + numOfPlayers.toString()
        numBallsPicker!!.value = numBallsPerPlayer
        numBallsView!!.text = "Number of Balls: " + numBallsPicker!!.value.toString()
    }

    private fun unpublishMessages() {
        messages.forEach {message -> messageClient!!.unpublish(message)}
    }

    private fun updateUI() {
        numBallsPicker!!.maxValue = possibleNumbers.size / numOfPlayers
        numPlayersView!!.text = "Number of players: " + numOfPlayers.toString()
        numBallsView!!.text = "Number of Balls: " + numBallsPicker!!.value.toString()
        for ((id, timestamp) in playerIDs) {
            Log.i(TAG, id + ": " + timestamp)
        }
    }

    private fun parseMessage(message: Message) {
        var content = String(message.content).split(':')
        if (content[0] == "ARBITRATION") {
            // Arbitration phase.
            if (!playerIDs.contains(content[1])) {
                playerIDs[content[1]] = content[2]
                numOfPlayers++
                updateUI()
            }

            if (selfTimestamp != "") {
                arbitrate()
            }

        } else if (content[0] == "DEALING") {
            if ((content[1] == UUID) && !dealingMode) {
                balls = content[2].split(',').toMutableList()
                displayBalls(balls!!)
            }
        }
    }

    private fun arbitrate() {
        var win = true
        playerIDs.forEach { _, timestamp -> if (timestamp.toLong() < selfTimestamp.toLong()) win = false}

        if (win) {
            dealingMode = true
            enableDealerUI()
        } else {
            dealingMode = false
            disableDealerUI()
        }
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
        imageLayout!!.removeAllViews()
        generateNumbers()

        // Send out numbers to other players
        val listOfPlayerIDs = playerIDs.keys.toList()
        for (i in 0 until listOfPlayerIDs.size) {
            var dealtNumbersPerPlayer = ""
            for (j in 0 until numBallsPerPlayer) {
                dealtNumbersPerPlayer += dealtNumbers[i*listOfPlayerIDs.size + j]
                if (j != numBallsPerPlayer-1) {
                    dealtNumbersPerPlayer += ","
                }
            }
            val byteArrayMessage = "$DEALING:${listOfPlayerIDs[i]}:$dealtNumbersPerPlayer".toByteArray()
            messages.add(Message(byteArrayMessage))
            messageClient!!.publish(messages.last())
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

    private fun findPlayers() {
        toast("Finding Players...")

        selfTimestamp = currentTimeMillis().toString()
        val byteArrayMessage = "$ARBITRATION:$UUID:$selfTimestamp".toByteArray()
        messages.add(Message(byteArrayMessage))
        messageClient!!.publish(messages.last())
        findPlayersBtn!!.text = LEAVE
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
}
