# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# -------------------------------------------------------------
# REGLAS ORIGINALES DEL PROYECTO (Tus reglas existentes)
# -------------------------------------------------------------
# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# -------------------------------------------------------------
# REGLAS CRÍTICAS PARA GSON Y CLASES DE DATOS DE TARIFAS
# -------------------------------------------------------------

# 1. MANTENER NOMBRES DE LAS CLASES DE MODELO (Ciudad y CostDetail):
#    Esto es crucial para que Gson sepa cómo deserializar el JSON que lee del Storage.
-keepnames class com.PatagonicApps.calculaya.Ciudad { *; }
-keepnames class com.PatagonicApps.calculaya.CostDetail { *; }

# 2. MANTENER LOS CAMPOS DE TODAS LAS CLASES QUE USAN GSON:
#    Si no mantenemos los campos (como 'nombre', 'puntoRetiroDia', etc.),
#    Gson fallará al mapear el JSON al objeto de Kotlin.
-keepclassmembers class com.PatagonicApps.calculaya.Ciudad {
    <fields>;
}
-keepclassmembers class com.PatagonicApps.calculaya.CostDetail {
    <fields>;
}

# 3. REGLA GENERAL DE GSON:
#    Asegura que el serializador funcione en general sin depender de anotaciones.
-keep class sun.misc.Unsafe { *; }