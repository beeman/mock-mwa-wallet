/*
 * Copyright (c) 2022 Solana Mobile Inc.
 */

package com.solana.mwallet

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.solana.mobilewalletadapter.walletlib.association.RemoteAssociationUri
import com.solana.mwallet.databinding.ActivityMainBinding
import com.solana.mwallet.usecase.UserAuthenticationUseCase

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.publicKeyText.text = getPublicKeyText()

        viewBinding.button.setOnClickListener { onAuthenticationButtonClicked() }

        updateAuthenticationButton()

        intent.data?.let { uri ->
            runCatching {
                val remoteAssociationUri = RemoteAssociationUri(uri)
                startActivity(
                    Intent(applicationContext, MobileWalletAdapterActivity::class.java)
                        .setData(remoteAssociationUri.uri))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateAuthenticationButton()
    }

    private fun onAuthenticationButtonClicked() {
        if (UserAuthenticationUseCase.isAuthenticated(this)) {
            UserAuthenticationUseCase.unauthenticate(this)
            Toast.makeText(applicationContext, "Unauthenticated", Toast.LENGTH_SHORT).show()
            updateAuthenticationButton()
        } else {
            UserAuthenticationUseCase.authenticate(this) { result , error ->
                Toast.makeText(applicationContext,
                    result?.let { "Authentication succeeded!" }
                        ?: error?.let { "Authentication error: $it" }
                        ?: "Authentication failed", Toast.LENGTH_SHORT)
                    .show()
                updateAuthenticationButton()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_qr_scanner -> {
                // Open ML Kit Barcode Scanner
                openBarcodeScanner()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openBarcodeScanner() {
        startActivity(Intent(applicationContext, BarcodeScannerActivity::class.java))
    }

    private fun getPublicKeyText(): String =
        runCatching { LocalKeypair.getPublicKey() }.getOrNull()
            ?.let { "Public key: $it" }
            ?: "Public key not configured"

    private fun updateAuthenticationButton() {
        viewBinding.button.setText(
            if (UserAuthenticationUseCase.isAuthenticated(this)) {
                R.string.label_unauthenticate
            } else {
                R.string.label_authenticate
            }
        )
    }
}
