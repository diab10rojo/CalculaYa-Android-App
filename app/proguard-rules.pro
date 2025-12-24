# ============================
# CONFIGURACIÓN BALANCEADA
# ============================

# 1. CONFIGURACIÓN GLOBAL
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes InnerClasses

# 2. GSON
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# 3. ✅ SOLO TUS MODELOS DE DATOS (lo crítico)
-keep class com.PatagonicApps.calculaya.Ciudad { *; }
-keep class com.PatagonicApps.calculaya.CostDetail { *; }
-keep class com.PatagonicApps.calculaya.CalculationResult { *; }

# Mantener @SerializedName
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 4. ✅ EL RESTO SE OFUSCA (protección)
# RateManager, RateCalculator, ConfigActivity, etc.
# se OFUSCAN pero manteniendo métodos públicos
-keepclassmembers class com.PatagonicApps.calculaya.RateManager {
    public <methods>;
}
-keepclassmembers class com.PatagonicApps.calculaya.RateCalculator {
    public <methods>;
}
-keepclassmembers class com.PatagonicApps.calculaya.RemoteConfigHelper {
    public <methods>;
}

# 5. ACTIVITIES
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# 6. FIREBASE
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# 7. ADMOB
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# 8. KOTLIN
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }

# 9. OPTIMIZACIONES LIGERAS
-optimizationpasses 3
-dontpreverify