package com.example.config

/**
 * Editable Monetization & Tip Jar Configuration for PageNest.
 *
 * Update the URLs, labels, recommendation timing, and optional supporter keys below
 * before building your release APK / AAB. Any entry with a blank `url` is hidden
 * automatically in the UI.
 */
data class TipLinkOption(
  val id: String,
  val labelEn: String,
  val labelFa: String,
  val subtitleEn: String,
  val subtitleFa: String,
  val url: String,
  val isIranRegion: Boolean,
)

object MonetizationConfig {

  // ---------------------------------------------------------------------------
  // 1) Tip Jar Links — Persian / Inside Iran (Rial / Local Gateways)
  // Replace these URLs with your actual Zarinpal, IDPay, Cofebede, PingPay, etc.
  // Leave `url = ""` on any unused slot to hide it.
  // ---------------------------------------------------------------------------
  val iranTipLinks: List<TipLinkOption> =
    listOf(
      TipLinkOption(
        id = "iran_primary",
        labelEn = "Support via Persian Gateway",
        labelFa = "حمایت با درگاه ریالی (ایران)",
        subtitleEn = "Direct Rial support link (editable in MonetizationConfig.kt)",
        subtitleFa = "لینک حمایت مستقیم برای کاربران داخل ایران",
        url = "https://mrambrosia.ir/support-fa",
        isIranRegion = true,
      ),
      TipLinkOption(
        id = "iran_coffee",
        labelEn = "Buy Me a Tea (Iran)",
        labelFa = "مهمان کردن یک چای / قهوه",
        subtitleEn = "Optional second local tip link (Cofebede / PingPay)",
        subtitleFa = "لینک دوم اختیاری (کافه‌بده، زرین‌پال یا پی‌پینگ)",
        url = "",
        isIranRegion = true,
      ),
    )

  // ---------------------------------------------------------------------------
  // 2) Tip Jar Links — Outside Iran (International)
  // Replace with your Ko-fi, Buy Me a Coffee, GitHub Sponsors, PayPal, or site URL.
  // ---------------------------------------------------------------------------
  val globalTipLinks: List<TipLinkOption> =
    listOf(
      TipLinkOption(
        id = "global_primary",
        labelEn = "Support Outside Iran (Ko-fi / Sponsors)",
        labelFa = "حمایت از خارج از ایران (Ko-fi / بین‌المللی)",
        subtitleEn = "Card, PayPal, or international sponsor link",
        subtitleFa = "پرداخت بین‌المللی با کارت یا پی‌پال",
        url = "https://mrambrosia.ir/support",
        isIranRegion = false,
      ),
      TipLinkOption(
        id = "global_github",
        labelEn = "GitHub Sponsors / Star",
        labelFa = "حمایت در گیت‌هاب",
        subtitleEn = "github.com/AmirSenpai",
        subtitleFa = "حمایت و معرفی در مخزن گیت‌هاب",
        url = "https://github.com/AmirSenpai",
        isIranRegion = false,
      ),
    )

  // ---------------------------------------------------------------------------
  // 3) PageNest Pro (Premium Upgrade) Links & Pricing Labels
  // Set your Myket / Play / Direct checkout URLs here.
  // ---------------------------------------------------------------------------
  const val PRO_PURCHASE_URL_IRAN: String = "https://myket.ir/app/ir.pagenest.mrambrosia"
  const val PRO_PRICE_LABEL_IRAN: String = "مایکت / ارتقا به نسخه حرفه‌ای"

  const val PRO_PURCHASE_URL_GLOBAL: String = "https://mrambrosia.ir/pagenest-pro"
  const val PRO_PRICE_LABEL_GLOBAL: String = "Get PageNest Pro"

  // Optional supporter activation codes you can share with donors / buyers
  // (e.g. from Ko-fi, Zarinpal, or Patreon receipts). Case-insensitive.
  val acceptedSupporterCodes: Set<String> =
    setOf(
      "PAGENEST-PRO",
      "AMIRSENPAI-SUPPORTER",
    )

  // ---------------------------------------------------------------------------
  // 4) Tip Jar Recommendation Timing (in minutes of active reading)
  // ---------------------------------------------------------------------------
  const val FIRST_PROMPT_AFTER_READING_MINUTES: Int = 15
  const val RANDOM_INTERVAL_MIN_MINUTES: Int = 25
  const val RANDOM_INTERVAL_MAX_MINUTES: Int = 55
  const val DEFAULT_FIXED_INTERVAL_MINUTES: Int = 30
  const val MIN_COOLDOWN_HOURS_BETWEEN_PROMPTS: Int = 12

  fun computeNextPromptDelaySeconds(
    isRandomMode: Boolean,
    fixedMinutes: Int,
    randomRoll: Double = Math.random(),
  ): Long {
    val minutes =
      if (isRandomMode) {
        val span = (RANDOM_INTERVAL_MAX_MINUTES - RANDOM_INTERVAL_MIN_MINUTES).coerceAtLeast(1)
        RANDOM_INTERVAL_MIN_MINUTES + (randomRoll.coerceIn(0.0, 0.9999) * (span + 1)).toInt()
      } else {
        fixedMinutes.coerceIn(10, 240)
      }
    return minutes * 60L
  }

  fun isValidSupporterCode(input: String): Boolean {
    val normalized = input.trim().uppercase()
    return normalized.isNotBlank() && acceptedSupporterCodes.any { it.equals(normalized, ignoreCase = true) }
  }

  fun activeIranTipLinks(): List<TipLinkOption> = iranTipLinks.filter { it.url.isNotBlank() }

  fun activeGlobalTipLinks(): List<TipLinkOption> = globalTipLinks.filter { it.url.isNotBlank() }
}
