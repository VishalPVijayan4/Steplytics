package com.buildndeploy.steplytics.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buildndeploy.steplytics.ui.components.SteplyticsScaffold
import com.buildndeploy.steplytics.ui.theme.CardBackground
import com.buildndeploy.steplytics.ui.theme.CardBorder
import com.buildndeploy.steplytics.ui.theme.ErrorRed
import com.buildndeploy.steplytics.ui.theme.FieldBackground
import com.buildndeploy.steplytics.ui.theme.PrimaryBlue
import com.buildndeploy.steplytics.ui.theme.PrimaryGreen
import com.buildndeploy.steplytics.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun UserSetupScreen(
    state: UserSetupState,
    sideEffects: Flow<UserSetupSideEffect>,
    onIntent: (UserSetupIntent) -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(sideEffects) {
        sideEffects.collectLatest { effect ->
            when (effect) {
                UserSetupSideEffect.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    SteplyticsScaffold(modifier = modifier) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 36.dp, start = 4.dp)
            ) {
                Text(
                    text = "Set Up Your Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Help us personalize your fitness experience.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }

            ProfileFieldCard(
                title = "Name",
                value = state.name,
                placeholder = "Enter your full name",
                supportingText = state.nameError,
                onValueChange = { onIntent(UserSetupIntent.OnValueChanged(UserProfileField.NAME, it)) },
                keyboardOptions = KeyboardOptions.Default
            )

            ProfileFieldCard(
                title = "Email",
                value = state.email,
                placeholder = "Enter your email address",
                supportingText = state.emailError,
                onValueChange = { onIntent(UserSetupIntent.OnValueChanged(UserProfileField.EMAIL, it)) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
            )

            ProfileFieldCard(
                title = "Age",
                value = state.age,
                placeholder = "Enter your age",
                supportingText = state.ageError,
                onValueChange = { onIntent(UserSetupIntent.OnValueChanged(UserProfileField.AGE, it)) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )

            ProfileFieldCard(
                title = "Weight",
                value = state.weight,
                placeholder = "Enter weight in kg",
                supportingText = state.weightError,
                onValueChange = { onIntent(UserSetupIntent.OnValueChanged(UserProfileField.WEIGHT, it)) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            )

            ProfileFieldCard(
                title = "Height",
                value = state.height,
                placeholder = "Enter height in cm",
                supportingText = state.heightError,
                onValueChange = { onIntent(UserSetupIntent.OnValueChanged(UserProfileField.HEIGHT, it)) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            )

            GenderSelector(
                selectedGender = state.gender,
                error = state.genderError,
                onGenderSelected = { onIntent(UserSetupIntent.OnGenderSelected(it)) }
            )

            Text(
                text = "Privacy notice: Steplytics does not store your data on our servers. Your data stays on this device. If you uninstall the app or clear app cache/data, your information will be lost forever.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Button(
                onClick = { onIntent(UserSetupIntent.OnContinueClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                enabled = !state.isSaving,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen)),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.isSaving) "Saving..." else "Continue",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFieldCard(
    title: String,
    value: String,
    placeholder: String,
    supportingText: String?,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(22.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(text = placeholder, color = TextSecondary)
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = FieldBackground,
                focusedContainerColor = FieldBackground,
                unfocusedBorderColor = CardBorder,
                focusedBorderColor = PrimaryBlue,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
                cursorColor = PrimaryBlue
            ),
            keyboardOptions = keyboardOptions,
            supportingText = if (supportingText == null) null else ({
                Text(text = supportingText, color = ErrorRed)
            })
        )
    }
}

@Composable
private fun GenderSelector(
    selectedGender: String,
    error: String?,
    onGenderSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(22.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Gender",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Male", "Female", "Other").forEach { option ->
                val selected = option == selectedGender
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onGenderSelected(option) }
                        .background(
                            if (selected) PrimaryBlue else FieldBackground,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        if (error != null) {
            Text(text = error, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}
