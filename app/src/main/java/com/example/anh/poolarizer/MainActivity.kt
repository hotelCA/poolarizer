package com.example.anh.poolarizer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings.Secure
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.Message
import com.google.android.gms.nearby.messages.MessageListener
import com.google.android.gms.nearby.messages.MessagesClient
import java.lang.System.currentTimeMillis
import java.util.*
import java.util.UUID.randomUUID

class MainActivity : AppCompatActivity() {

    // "Constants"
    private val TAG = "ETM"
    private val UUID = randomUUID().toString()
    private var ANDROID_ID = ""
    private val ARBITRATION = "ARBITRATION"
    private val DEALING = "DEALING"
    private val RESET = "Reset"
    private val FIND_PLAYERS = "Find Players"
    private val DEAL = "Deal"

    // Game related variables
    private var numOfPlayers = 1
    private var numBallsPerPlayer = 2
    private var possibleNumbers = Array<String>(15, {i -> (i+1).toString()})
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
    private var ballsView: TextView? = null
    private var numBallsBar: SeekBar? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ANDROID_ID = Secure.getString(contentResolver, Secure.ANDROID_ID)

        Log.i(TAG, ANDROID_ID)
        Log.i(TAG, UUID)

        numBallsBar  = findViewById(R.id.numBallsBar)
        numBallsBar!!.min = 1
        numBallsBar!!.max = possibleNumbers.size / numOfPlayers

        numPlayersView = findViewById(R.id.numPlayersView)
        numPlayersView!!.text = "Number of Players: " + numOfPlayers.toString()

        numBallsView = findViewById(R.id.numBallsView)
        numBallsView!!.text = numBallsBar!!.progress.toString()

        ballsView = findViewById(R.id.ballsView)
        ballsView!!.text = ""

        findPlayersBtn  = findViewById(R.id.findPlayersBtn)
        findPlayersBtn!!.text = FIND_PLAYERS

        dealBtn = findViewById(R.id.dealBtn)
        dealBtn!!.text = DEAL

        numBallsBar!!.setOnSeekBarChangeListener (object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                           fromUser: Boolean) {
                if (fromUser) {
                    numBallsView!!.text = progress.toString()
                    numBallsPerPlayer = progress.toInt()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // called when tracking the seekbar is started
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // called when tracking the seekbar is stopped
            }
        })

        findPlayersBtn!!.setOnClickListener {
            if (findPlayersBtn!!.text.equals(FIND_PLAYERS)) {
                findPlayers()
            } else if (findPlayersBtn!!.text.equals(RESET)) {
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
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        unpublishMessages()
        messageClient!!.unsubscribe(messageListener!!)
        super.onStop()
    }

    private fun reset() {
        unpublishMessages()
        dealingMode = false

        numOfPlayers = 1
        numBallsPerPlayer = 2
        findPlayersBtn!!.text = FIND_PLAYERS
        dealtNumbers.clear()
        balls = null
        selfTimestamp = ""
        playerIDs.clear()

        numPlayersView!!.text = "Number of Players: " + numOfPlayers.toString()
        numBallsBar!!.progress = numBallsPerPlayer
        numBallsView!!.text = numBallsBar!!.progress.toString()
        ballsView!!.text = ""
    }

    private fun unpublishMessages() {
        messages.forEach {message -> messageClient!!.unpublish(message)}
    }

    private fun updateUI() {
        numBallsBar!!.max = possibleNumbers.size / numOfPlayers
        numPlayersView!!.text = "Number of players: " + numOfPlayers.toString()
        numBallsView!!.text = numBallsBar!!.progress.toString()
        for ((id, timestamp) in playerIDs) {
            Log.i(TAG, id + ": " + timestamp)
        }
    }

    private fun parseMessage(message: Message) {
        var content = String(message.content).split(':')
        if (content[0].equals("ARBITRATION")) {
            // Arbitration phase.
            if (!selfTimestamp.equals("")) {
                // Make sure we send our arbitration token first.
                if (!playerIDs.contains(content[1])) {
                    playerIDs[content[1]] = content[2]
                    numOfPlayers++
                    updateUI()
                }
                arbitrate()
            }
        } else if (content[0].equals("DEALING")) {
            if (content[1].equals(UUID) && !dealingMode) {
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
//        findPlayersBtn!!.isEnabled = false
        dealBtn!!.isEnabled = true
        numBallsBar!!.isEnabled = true
    }

    private fun disableDealerUI() {
//        findPlayersBtn!!.isEnabled = false
        dealBtn!!.isEnabled = false
        numBallsBar!!.isEnabled = false
    }

    private fun initUI(){
        dealBtn!!.isEnabled = false
        numBallsBar!!.isEnabled = false
    }
    private fun dealNumbers() {
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
            val byteArrayMessage = (DEALING + ":" + listOfPlayerIDs[i] + ":" + dealtNumbersPerPlayer).toByteArray()
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
        var displayText = ""
        for (ball in balls) {

            displayText += ball + " "
        }
        ballsView!!.text = displayText
    }

    private fun findPlayers() {
        toast("Finding Players...")

        selfTimestamp = currentTimeMillis().toString()
        val byteArrayMessage = (ARBITRATION + ":" + UUID + ":" + selfTimestamp).toByteArray()
        messages.add(Message(byteArrayMessage))
        messageClient!!.publish(messages.last())
        findPlayersBtn!!.text = RESET
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
