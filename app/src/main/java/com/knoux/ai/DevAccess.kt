package com.knoux.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Developer Access System
 * نظام دخول المطور - مؤقت
 * 
 * Password: knoux
 * لتفعيل جميع ميزات Pro بدون قيود
 */

internal object DevAccessManager {
    private const val DEV_PASSWORD = "knoux"
    private var _isDevModeActive = mutableStateOf(false)
    
    val isDevModeActive: Boolean
        get() = _isDevModeActive.value
    
    fun authenticate(password: String): Boolean {
        val success = password == DEV_PASSWORD
        if (success) {
            _isDevModeActive.value = true
        }
        return success
    }
    
    fun deactivate() {
        _isDevModeActive.value = false
    }
    
    // Pro Status for each section
    fun hasProAccess(sectionId: String): Boolean {
        return _isDevModeActive.value
    }
    
    // Check if user has any Pro feature
    val hasAnyProAccess: Boolean
        get() = _isDevModeActive.value
}

/**
 * Hidden Developer Access Dialog
 * يظهر من خلال 5 ضغطات سريعة على شعار التطبيق
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DevAccessDialog(
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var pendingAuth by remember { mutableStateOf(false) }

    // Auto-dismiss after successful auth
    LaunchedEffect(pendingAuth) {
        if (pendingAuth) {
            delay(1200)
            onAuthenticated()
        }
    }

    fun tryAuth() {
        if (DevAccessManager.authenticate(password)) {
            successMessage = "✅ Access Granted! All Pro features unlocked."
            errorMessage = null
            password = ""
            pendingAuth = true
        } else {
            errorMessage = "❌ Incorrect password. Try again."
            successMessage = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xff0a0520),
        contentColor = Chalk,
        tonalElevation = 0.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xff1a0040),
                                Color(0xff002040),
                                Color(0xff001a30)
                            )
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .border(
                        2.dp,
                        Brush.horizontalGradient(
                            listOf(
                                Gold.copy(alpha = 0.8f),
                                Cyan.copy(alpha = 0.5f),
                                Gold.copy(alpha = 0.8f)
                            )
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Glyph(Mark.KEY, Modifier.size(48.dp), Gold, true)
                    Label(
                        (Copy(R.string.k_developer_access)).localized(),
                        20,
                        Gold,
                        FontWeight.ExtraBold
                    )
                    Label(
                        (Copy(R.string.k_enter_password_to_unlock_all_pro_features)).localized(),
                        12,
                        Mist.copy(alpha = 0.7f),
                        align = TextAlign.Center
                    )
                }
            }

            // Password Input
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Label(
                    (Copy(R.string.k_password_2)).localized(),
                    13,
                    Chalk,
                    FontWeight.Medium
                )
                
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xff1a0d30), RoundedCornerShape(14.dp))
                        .border(
                            1.5.dp,
                            if (errorMessage != null) Pink.copy(alpha = 0.8f)
                            else if (successMessage != null) Mint.copy(alpha = 0.8f)
                            else Violet.copy(alpha = 0.5f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    BasicTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMessage = null
                            successMessage = null
                        },
                        textStyle = TextStyle(
                            color = Chalk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { tryAuth() }
                        ),
                        cursorBrush = SolidColor(Cyan),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    ) { innerTextField ->
                        if (password.isEmpty()) {
                            Label(
                                (Copy(R.string.k_enter_password)).localized(),
                                16,
                                Mist.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                }

                // Error/Success Message
                if (errorMessage != null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Glyph(Mark.WARN, Modifier.size(16.dp), Pink, false)
                        Label(errorMessage!!, 12, Pink)
                    }
                }
                
                if (successMessage != null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Glyph(Mark.CHECK, Modifier.size(16.dp), Mint, true)
                        Label(successMessage!!, 12, Mint)
                    }
                }
            }

            // Authenticate Button
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Gold.copy(alpha = 0.3f), Cyan.copy(alpha = 0.3f))
                        ),
                        RoundedCornerShape(14.dp)
                    )
                    .border(1.5.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .clickable(
                        enabled = password.isNotEmpty(),
                        role = Role.Button
                    ) { tryAuth() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Glyph(Mark.KEY, Modifier.size(20.dp), Gold, true)
                    Label(
                        (Copy(R.string.k_authenticate)).localized(),
                        15,
                        if (password.isEmpty()) Mist.copy(alpha = 0.5f) else Gold,
                        FontWeight.Bold
                    )
                }
            }

            // Info note
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xff0d0028).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Label(
                    (Copy(R.string.k_this_is_a_temporary_developer_access_for_testing_purpose)).localized(),
                    11,
                    Mist.copy(alpha = 0.7f),
                    align = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Dev Mode Indicator Badge
 * شارة تظهر في الأعلى عندما يكون وضع المطور مفعل
 */
@Composable
internal fun DevModeIndicator() {
    if (DevAccessManager.isDevModeActive) {
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.3f), Cyan.copy(alpha = 0.3f))),
                    RoundedCornerShape(8.dp)
                )
                .border(1.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Glyph(Mark.CROWN, Modifier.size(14.dp), Gold, true)
                Label(
                    (Copy(R.string.k_dev_mode)).localized(),
                    10,
                    Gold,
                    FontWeight.Bold
                )
            }
        }
    }
}
