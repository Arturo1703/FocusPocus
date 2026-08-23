package com.arturolopez.focuspocus.blocking

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager

data class InstalledLaunchableApp(
    val packageName: String,
    val name: String,
    val applicationInfo: ApplicationInfo,
    val category: AppBlockCategory,
)

object InstalledAppsCatalog {

    fun loadBlockableApps(context: Context): List<InstalledLaunchableApp> {
        val packageManager = context.packageManager
        val protectedPackages = protectedPackages(context)
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager.queryLauncherActivities(launcherIntent)
            .asSequence()
            .mapNotNull { resolveInfo -> resolveInfo.activityInfo?.applicationInfo }
            .filter(ApplicationInfo::enabled)
            .distinctBy(ApplicationInfo::packageName)
            .filterNot { app -> app.packageName in protectedPackages }
            .filterNot { app -> hasProtectedPackageMarker(app.packageName) }
            .map { app ->
                val name = packageManager.getApplicationLabel(app).toString()
                InstalledLaunchableApp(
                    packageName = app.packageName,
                    name = name,
                    applicationInfo = app,
                    category = categorize(app, name),
                )
            }
            .sortedWith(compareBy(InstalledLaunchableApp::category, InstalledLaunchableApp::name))
            .toList()
    }

    private fun protectedPackages(context: Context): Set<String> {
        val packageManager = context.packageManager
        return buildSet {
            add(context.packageName)
            addAll(ALWAYS_PROTECTED_PACKAGES)

            context.getSystemService(TelecomManager::class.java)
                ?.defaultDialerPackage
                ?.let(::add)
            Telephony.Sms.getDefaultSmsPackage(context)?.let(::add)

            listOf(
                Intent(Settings.ACTION_SETTINGS),
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")),
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI),
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            ).forEach { intent ->
                packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo
                    ?.packageName
                    ?.let(::add)
            }
        }
    }

    private fun categorize(app: ApplicationInfo, name: String): AppBlockCategory {
        val searchable = "${app.packageName} $name".lowercase()
        return when {
            app.category == ApplicationInfo.CATEGORY_GAME ||
                app.flags and ApplicationInfo.FLAG_IS_GAME != 0 -> AppBlockCategory.GAMES

            SOCIAL_MARKERS.any(searchable::contains) ||
                app.category == ApplicationInfo.CATEGORY_SOCIAL -> AppBlockCategory.SOCIAL

            SHOPPING_MARKERS.any(searchable::contains) -> AppBlockCategory.SHOPPING

            ENTERTAINMENT_MARKERS.any(searchable::contains) ||
                app.category == ApplicationInfo.CATEGORY_AUDIO ||
                app.category == ApplicationInfo.CATEGORY_VIDEO -> AppBlockCategory.ENTERTAINMENT

            else -> AppBlockCategory.OTHER
        }
    }

    private fun hasProtectedPackageMarker(packageName: String): Boolean {
        val normalized = packageName.lowercase()
        return PROTECTED_PACKAGE_MARKERS.any(normalized::contains)
    }

    @Suppress("DEPRECATION")
    private fun PackageManager.queryLauncherActivities(intent: Intent) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            queryIntentActivities(intent, 0)
        }

    private val ALWAYS_PROTECTED_PACKAGES = setOf(
        "com.android.settings",
        "com.android.systemui",
        "com.android.phone",
        "com.android.contacts",
        "com.android.vending",
        "com.android.server.telecom",
        "com.android.permissioncontroller",
        "com.google.android.GoogleCamera",
        "com.google.android.apps.docs",
        "com.google.android.apps.maps",
        "com.google.android.apps.messaging",
        "com.google.android.apps.photos",
        "com.google.android.apps.walletnfcrel",
        "com.google.android.calendar",
        "com.google.android.permissioncontroller",
        "com.google.android.dialer",
        "com.google.android.contacts",
        "com.google.android.deskclock",
        "com.google.android.googlequicksearchbox",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.calendar",
        "com.samsung.android.dialer",
        "com.samsung.android.contacts",
        "com.samsung.android.honeyboard",
        "com.samsung.android.incallui",
        "com.samsung.android.messaging",
        "com.samsung.android.spay",
        "com.samsung.android.spayfw",
        "com.samsung.android.app.telephonyui",
        "com.samsung.android.emergency",
        "com.sec.android.app.camera",
        "com.sec.android.app.clockpackage",
        "com.sec.android.app.myfiles",
    )

    private val PROTECTED_PACKAGE_MARKERS = listOf(
        ".emergency",
        ".dialer",
        ".contacts",
        ".incallui",
        ".telecom",
        ".telephony",
        ".permissioncontroller",
        ".systemui",
        ".launcher",
    )

    private val SOCIAL_MARKERS = listOf(
        "instagram",
        "facebook",
        "twitter",
        "tiktok",
        "snapchat",
        "reddit",
        "linkedin",
        "discord",
        "telegram",
        "whatsapp",
        "pinterest",
        "threads",
        "bluesky",
        "mastodon",
    )

    private val SHOPPING_MARKERS = listOf(
        "shopping",
        "amazon.mshop",
        "aliexpress",
        "temu",
        "shein",
        "ebay",
        "wallapop",
        "vinted",
        "wish",
        "etsy",
        "zalando",
        "mercadolibre",
    )

    private val ENTERTAINMENT_MARKERS = listOf(
        "youtube",
        "netflix",
        "spotify",
        "twitch",
        "disney",
        "primevideo",
        "crunchyroll",
        "podcast",
        "music",
        "video",
        "player",
    )
}
