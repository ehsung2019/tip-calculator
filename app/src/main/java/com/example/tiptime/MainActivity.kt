/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.example.tiptime

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptime.ui.theme.TipTimeTheme
import java.text.NumberFormat
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this) { initializationStatus -> }

        setContent {
            TipTimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        TipTimeLayout(adUnitId = "ca-app-pub-8163475936982739/1267348899")

                        // Display traditional AdView
                        AndroidView(
                            factory = { context ->
                                AdView(context).apply {
                                    // Use setAdSize method here
                                    setAdSize(AdSize.BANNER)
                                    adUnitId = "ca-app-pub-8163475936982739/1267348899" // Set the ad unit ID here
                                    loadAd(AdRequest.Builder().build())
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun BannerAdView(adUnitId: String) {
    val context = LocalContext.current

    // Use AndroidView to integrate the AdView
    AndroidView(
        factory = { context ->
            AdView(context).apply {
                // Use setAdSize method here
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId // This is mutable, and you are setting it correctly
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}



@Composable
fun EditNumberField(value:String,
                    onValueChange:(String)->Unit,
                    modifier:Modifier=Modifier){

    TextField(
        value=value,
        onValueChange=onValueChange,
        modifier=modifier
            .padding(bottom=32.dp)
            .fillMaxWidth(),
        label={Text(stringResource(R.string.bill_amount))},
        singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
    )
}

@Composable
fun EditTipPercentageField(value: String,
                           onValueChange: (String) -> Unit,
                           modifier: Modifier = Modifier) {
    // Track whether the field is focused
    var isFocused by remember { mutableStateOf(false) }

    TextField(
        value = value,
        onValueChange = { newValue ->
            // Remove any non-numeric characters, except the percent sign
            val cleanedValue = newValue.filter { it.isDigit() || it == '%' }
            onValueChange(cleanedValue)
        },
        modifier = modifier
            .padding(bottom = 32.dp)
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                // If the field loses focus and is empty, reset to default value
                if (!isFocused && value.isEmpty()) {
                    onValueChange("15%")
                }
            },
        label = { Text(stringResource(R.string.tip_percentage)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}



@Composable
fun TipTimeLayout(adUnitId: String) {
    var amountInput by remember { mutableStateOf("") }
    var percentageInput by remember { mutableStateOf("15%") }

    var amount = amountInput.toDoubleOrNull() ?: 0.0
    val percentageWithoutPercent = percentageInput.removeSuffix("%").toDoubleOrNull() ?: 15.0

    val tip = calculateTip(amount, percentageWithoutPercent)

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 40.dp)
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.calculate_tip),
            modifier = Modifier
                .padding(bottom = 16.dp, top = 40.dp)
                .align(alignment = Alignment.Start)
        )
        EditNumberField(
            value = amountInput,
            onValueChange = { amountInput = it },
            modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
        )

        EditTipPercentageField(
            value = if (percentageInput.endsWith("%")) percentageInput else "${percentageInput}%",
            onValueChange = { percentageInput = it },
            modifier = Modifier.padding(bottom = 32.dp).fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.tip_amount, tip),
            style = MaterialTheme.typography.displaySmall
        )

        // Add the Banner Ad here
        BannerAdView(adUnitId = adUnitId)

        Spacer(modifier = Modifier.height(150.dp))
    }
}


/**
 * Calculates the tip based on the user input and format the tip amount
 * according to the local currency.
 * Example would be "$10.00".
 */
private fun calculateTip(amount: Double, tipPercent: Double = 15.0): String {
    val tip = tipPercent / 100 * amount
    return NumberFormat.getCurrencyInstance().format(tip)
}

@Preview(showBackground = true)
@Composable
fun TipTimeLayoutPreview() {
    TipTimeTheme {
        TipTimeLayout(adUnitId = "ca-app-pub-8163475936982739/1267348899")
    }
}
