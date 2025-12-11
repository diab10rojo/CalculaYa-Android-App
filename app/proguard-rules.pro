# ============================
# Blindar clases usadas por Gson
# ============================
-keep class com.PatagonicApps.calculaya.** { *; }

# Mantener clases específicas
-keep class com.PatagonicApps.calculaya.Ciudad { *; }
-keep class com.PatagonicApps.calculaya.CostDetail { *; }
-keep class com.PatagonicApps.calculaya.RateManager { *; }

# ============================
# Activities y Application
# ============================
-keep class com.PatagonicApps.calculaya.MainActivity { *; }
-keep class com.PatagonicApps.calculaya.ConfigActivity { *; }
-keep class com.PatagonicApps.calculaya.CalculayaApp { *; }

# ============================
# Preservar atributos y firmas genéricas
# ============================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses, EnclosingMethod

# ============================
# Mantener constructores y campos de data classes
# ============================
-keepclassmembers class com.PatagonicApps.calculaya.Ciudad {
    <fields>;
    public <init>(...);
}
-keepclassmembers class com.PatagonicApps.calculaya.CostDetail {
    <fields>;
    public <init>(...);
}

# ============================
# Regla genérica para Gson
# ============================
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================
# Blindar TypeToken (para evitar problemas en release)
# ============================
-keep class com.google.gson.reflect.TypeToken
-keepclassmembers class com.google.gson.reflect.TypeToken {
    <fields>;
    <methods>;
}
