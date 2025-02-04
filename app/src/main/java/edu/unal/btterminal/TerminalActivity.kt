package edu.unal.btterminal
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.unal.btterminal.adapter.MessageAdapter
import edu.unal.btterminal.bluetooth.BTManager
import edu.unal.btterminal.model.Message
import android.widget.ArrayAdapter
import android.widget.Spinner
import edu.unal.btterminal.utils.LineEnding

class TerminalActivity : AppCompatActivity() {
    private val btManager by lazy { BTManager.getInstance(this) }
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var spinnerLineEnding: Spinner
    private var selectedLineEnding = LineEnding.CRLF

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminal)

        // Initialize views
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        spinnerLineEnding = findViewById(R.id.spinnerLineEnding)

        setupLineEndingSpinner()

        // Setup RecyclerView
        messageAdapter = MessageAdapter()
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Messages start from bottom
        }
        rvMessages.adapter = messageAdapter

        // Setup click listeners
        setupClickListeners()

        // Start receiving messages
        startMessageReceiver()
    }

    private fun setupLineEndingSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            LineEnding.entries.map { it.displayName }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerLineEnding.adapter = adapter
        spinnerLineEnding.setSelection(LineEnding.entries.indexOf(LineEnding.CRLF))
        
        spinnerLineEnding.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLineEnding = LineEnding.entries.toTypedArray()[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedLineEnding = LineEnding.CRLF
            }
        }
    }

    private fun setupClickListeners() {
        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                if (btManager.sendData(message, selectedLineEnding)) {
                    messageAdapter.addMessage(Message(message, true))
                    etMessage.text.clear()
                    scrollToBottom()
                } else {
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startMessageReceiver() {
        btManager.startReceiving { message ->
            runOnUiThread {
                messageAdapter.addMessage(Message(message, false))
                scrollToBottom()
            }
        }
    }

    private fun scrollToBottom() {
        rvMessages.post {
            rvMessages.smoothScrollToPosition(messageAdapter.itemCount - 1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        btManager.closeConnection()
    }
} 