package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafliadipratama.musiceuy.R
import com.rafliadipratama.musiceuy.ui.theme.BrandGreen
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainerHigh
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import com.rafliadipratama.musiceuy.util.AppLanguage

@Composable
fun LanguageSelectionDialog(
    currentLanguageCode: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        titleContentColor = TextPrimary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrandGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = stringResource(id = R.string.language_dialog_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppLanguage.entries.forEach { lang ->
                    val isSelected = when (lang) {
                        AppLanguage.SYSTEM -> currentLanguageCode.isEmpty() || currentLanguageCode.equals("system", ignoreCase = true)
                        AppLanguage.INDONESIAN -> currentLanguageCode.equals("id", ignoreCase = true) || currentLanguageCode.equals("in", ignoreCase = true)
                        AppLanguage.ENGLISH -> currentLanguageCode.equals("en", ignoreCase = true)
                    }

                    LanguageOptionItem(
                        icon = when (lang) {
                            AppLanguage.SYSTEM -> Icons.Default.PhoneAndroid
                            AppLanguage.INDONESIAN -> Icons.Default.Language
                            AppLanguage.ENGLISH -> Icons.Default.Public
                        },
                        title = when (lang) {
                            AppLanguage.SYSTEM -> stringResource(id = R.string.language_system)
                            AppLanguage.INDONESIAN -> stringResource(id = R.string.language_indonesian)
                            AppLanguage.ENGLISH -> stringResource(id = R.string.language_english)
                        },
                        subtitle = when (lang) {
                            AppLanguage.SYSTEM -> stringResource(id = R.string.language_system_desc)
                            AppLanguage.INDONESIAN -> stringResource(id = R.string.language_indonesian_desc)
                            AppLanguage.ENGLISH -> stringResource(id = R.string.language_english_desc)
                        },
                        isSelected = isSelected,
                        onClick = { onSelectLanguage(lang.code) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.close),
                    color = BrandGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun LanguageOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) BrandGreen else Color.White.copy(alpha = 0.08f)
    val bgColor = if (isSelected) BrandGreen.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) BrandGreen else TextSecondary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isSelected) BrandGreen else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(BrandGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
