package com.example.ui.monetization

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.MonetizationConfig
import com.example.config.TipLinkOption
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.NotoNaskhFontFamily

@Composable
fun TipJarDialog(
  totalReadingMinutes: Int,
  isFromRecommendation: Boolean,
  isProUnlocked: Boolean,
  hasSupportedAlready: Boolean,
  isFa: Boolean,
  onMarkSupported: () -> Unit,
  onOpenProDialog: () -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  val iranLinks = remember { MonetizationConfig.activeIranTipLinks() }
  val globalLinks = remember { MonetizationConfig.activeGlobalTipLinks() }

  val openUrl: (String) -> Unit = { url ->
    runCatching {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(38.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Coffee,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        Column {
          Text(
            text = if (isFromRecommendation) strings.tipJarRecommendTitle else strings.tipJarTitle,
            fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = strings.tipJarSubtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = strings.tipJarRecommendBody(totalReadingMinutes),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (iranLinks.isNotEmpty()) {
          Text(
            text = strings.tipJarIranSection,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
          )
          iranLinks.forEach { option ->
            TipLinkCard(
              option = option,
              isFa = isFa,
              actionLabel = strings.openLinkAction,
              onClick = { openUrl(option.url) },
            )
          }
        }

        if (globalLinks.isNotEmpty()) {
          Text(
            text = strings.tipJarGlobalSection,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
          )
          globalLinks.forEach { option ->
            TipLinkCard(
              option = option,
              isFa = isFa,
              actionLabel = strings.openLinkAction,
              onClick = { openUrl(option.url) },
            )
          }
        }

        if (!isProUnlocked) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                  onDismiss()
                  onOpenProDialog()
                }
                .testTag("tip_jar_upgrade_pro_card"),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = strings.proHeroTitle,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = strings.proBenefitNoPrompts,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        if (!hasSupportedAlready && !isProUnlocked) {
          OutlinedButton(
            onClick = {
              onMarkSupported()
              onDismiss()
            },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().testTag("tip_jar_already_supported_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Favorite,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = strings.tipJarAlreadySupported,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("tip_jar_close_button"),
      ) {
        Text(
          text = if (isFromRecommendation) strings.tipJarRemindLater else strings.done,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    },
  )
}

@Composable
private fun TipLinkCard(
  option: TipLinkOption,
  isFa: Boolean,
  actionLabel: String,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .testTag("tip_link_${option.id}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = if (option.isIranRegion) Icons.Rounded.Storefront else Icons.Rounded.Public,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (isFa) option.labelFa else option.labelEn,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = if (isFa) option.subtitleFa else option.subtitleEn,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = actionLabel,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
          contentDescription = actionLabel,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(15.dp),
        )
      }
    }
  }
}

@Composable
fun PageNestProDialog(
  isProUnlocked: Boolean,
  isFa: Boolean,
  onRedeemSupporterCode: (String) -> Boolean,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  var supporterCode by remember { mutableStateOf("") }
  var codeError by remember { mutableStateOf<String?>(null) }

  val openUrl: (String) -> Unit = { url ->
    runCatching {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(38.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.WorkspacePremium,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(21.dp),
            )
          }
        }
        Column {
          Text(
            text = if (isProUnlocked) strings.proActiveTitle else strings.proTitle,
            fontFamily = if (isFa) NotoNaskhFontFamily else LiterataFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = if (isProUnlocked) strings.proBadge else strings.proHeroTitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (isProUnlocked) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = strings.proActiveSub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        } else {
          Text(
            text = strings.proHeroSub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = strings.proBenefitsTitle,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          listOf(
            strings.proBenefitNoPrompts,
            strings.proBenefitBadge,
            strings.proBenefitSupportDev,
            strings.proBenefitStoreReady,
          ).forEach { benefit ->
            Row(
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp).size(15.dp),
              )
              Text(
                text = benefit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        if (!isProUnlocked) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          if (MonetizationConfig.PRO_PURCHASE_URL_IRAN.isNotBlank()) {
            Button(
              onClick = { openUrl(MonetizationConfig.PRO_PURCHASE_URL_IRAN) },
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
              modifier = Modifier.fillMaxWidth().testTag("pro_upgrade_iran_button"),
            ) {
              Icon(Icons.Rounded.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = strings.proUpgradeIranAction,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                  text = MonetizationConfig.PRO_PRICE_LABEL_IRAN,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
              }
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            }
          }

          if (MonetizationConfig.PRO_PURCHASE_URL_GLOBAL.isNotBlank()) {
            OutlinedButton(
              onClick = { openUrl(MonetizationConfig.PRO_PURCHASE_URL_GLOBAL) },
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
              modifier = Modifier.fillMaxWidth().testTag("pro_upgrade_global_button"),
            ) {
              Icon(
                Icons.Rounded.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = strings.proUpgradeGlobalAction,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = MonetizationConfig.PRO_PRICE_LABEL_GLOBAL,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          Text(
            text = strings.proRedeemCodeLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = supporterCode,
              onValueChange = {
                supporterCode = it
                codeError = null
              },
              singleLine = true,
              placeholder = { Text(strings.proRedeemCodeHint, style = MaterialTheme.typography.bodySmall) },
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("pro_supporter_code_input"),
            )
            Button(
              onClick = {
                val ok = onRedeemSupporterCode(supporterCode)
                if (ok) {
                  codeError = null
                  onDismiss()
                } else {
                  codeError = strings.proInvalidCodeMessage
                }
              },
              enabled = supporterCode.isNotBlank(),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(50.dp).testTag("pro_activate_code_button"),
            ) {
              Text(strings.proRedeemAction)
            }
          }
          if (codeError != null) {
            Text(
              text = codeError!!,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.done, color = MaterialTheme.colorScheme.primary)
      }
    },
  )
}
