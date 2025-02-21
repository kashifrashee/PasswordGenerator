package com.example.passwordgenerator.ui.theme

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.passwordgenerator.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun PasswordGenerator(modifier: Modifier = Modifier) {
    var password = rememberSaveable { mutableStateOf("") }
    var strength = rememberSaveable { mutableStateOf("Weak") }
    var isGenerating = rememberSaveable { mutableStateOf(false) }
    val qrBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface) // Better background color
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔑 Password Generator",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(0.7f)
                            .padding(end = 8.dp),
                        value = if (isGenerating.value) "${password.value}|" else password.value,
                        onValueChange = {},
                        label = { Text("Password") },
                        readOnly = true,
                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                        singleLine = true,
                        trailingIcon = {
                            if (password.value.isNotEmpty()) {
                                IconButton(onClick = { copyToClipboard(password.value, context) }) {
                                    Icon(
                                        painter = painterResource(R.drawable.baseline_content_copy_24),
                                        contentDescription = "Copy"
                                    )
                                }
                            }
                        }
                    )

                    OutlinedTextField(
                        modifier = Modifier.weight(0.3f),
                        value = strength.value,
                        onValueChange = {},
                        label = { Text("Strength") },
                        readOnly = true,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = getStrengthColor(strength.value)
                        ),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isGenerating.value = true
                    password.value = ""
                    val finalPassword = generateRandomPassword(12)

                    for (i in finalPassword.indices) {
                        repeat(5) {
                            password.value = password.value.take(i) + getRandomChar() + password.value.drop(i + 1)
                            delay(50)
                        }
                        password.value = password.value.take(i) + finalPassword[i] + password.value.drop(i + 1)
                        delay(100)
                    }

                    strength.value = evaluatePasswordStrength(finalPassword)
                    val qrCode = generateQRCodeBitmap(finalPassword)
                    withContext(Dispatchers.Main) {
                        qrBitmap.value = qrCode.copy(qrCode.config ?: Bitmap.Config.ARGB_8888, true)
                    }

                    isGenerating.value = false
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Generate", modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Password", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { sharePasswordAndQRCode(context, password.value, qrBitmap.value) },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }


        Spacer(modifier = Modifier.height(16.dp))


        if (qrBitmap.value != null) {
            Card(
                modifier = Modifier
                    .size(220.dp)
                    .shadow(8.dp, shape = RoundedCornerShape(12.dp)), // Shadow & rounded corners
                shape = RoundedCornerShape(12.dp)
            ) {
                Image(
                    bitmap = qrBitmap.value!!.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp)) // Border around QR code
                )
            }
        }
    }
}


private fun generateQRCodeBitmap(text: String): Bitmap {
    val size = 512
    val qrCodeWriter = QRCodeWriter()
    val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size)

    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}

private fun getRandomChar(): Char {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()"
    return chars.random()
}

private fun generateRandomPassword(length: Int = 12): String {
    if (length < 4) return "Weak"

    val upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".random()
    val lowerCase = "abcdefghijklmnopqrstuvwxyz".random()
    val digit = "0123456789".random()
    val specialChar = "!@#\$%^&*()-_=+[]{}|;:'\",.<>?/".random()

    val remainingChars = (1..(length - 4))
        .map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*()-_=+[]{}|;:'\",.<>?/".random() }
        .toMutableList()

    val passwordList = mutableListOf(upperCase, lowerCase, digit, specialChar) + remainingChars
    return passwordList.shuffled().joinToString("")
}


private fun evaluatePasswordStrength(password: String): String {
    return when {
        password.length < 8 -> "Weak"
        password.length >= 12 && password.any { it.isDigit() } &&
                password.any { it.isUpperCase() } &&
                password.any { it in "!@#\$%^&*()-_=+[]{}|;:'\",.<>?/" } -> "Strong"
        password.length in 8..11 && password.any { it.isDigit() } &&
                password.any { it.isUpperCase() } &&
                password.any { it in "!@#\$%^&*()-_=+[]{}|;:'\",.<>?/" } -> "Medium"
        else -> "Weak"
    }
}


// Function to get strength color
@Composable
fun getStrengthColor(strength: String): Color {
    return when (strength) {
        "Weak" -> Color.Red
        "Medium" -> Color(0xFFFFA500) // Orange
        "Strong" -> Color.Green
        else -> MaterialTheme.colorScheme.onSurface
    }
}

// Function to copy password to clipboard
fun copyToClipboard(password: String, context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Password", password)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Password copied!", Toast.LENGTH_SHORT).show()
}

// function that shares the password & QR code
private fun sharePasswordAndQRCode(context: Context, password: String, qrBitmap: Bitmap?) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "🔐 Password: $password")

        qrBitmap?.let { bitmap ->
            val qrUri = saveBitmapToFile(context, bitmap)
            qrUri?.let { uri ->
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
    context.startActivity(Intent.createChooser(intent, "Share Password & QR Code"))
}

// function to save the QR code as an image file:
private fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val file = File(context.cacheDir, "qr_code.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        Log.d("PasswordGenerator", "QR code saved to ${file.absolutePath}")
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("PasswordGenerator", "Error saving QR code", e)
        null
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordGeneratorPreview() {
    PasswordGenerator()
}