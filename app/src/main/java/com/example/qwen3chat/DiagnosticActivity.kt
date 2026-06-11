package com.example.qwen3chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class DiagnosticActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostic)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Diagnostics"

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, com.example.qwen3chat.ui.diagnostics.DiagnosticFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
