# R8 / ProGuard rules for PaymentsLab-KMP release builds. Payment SDKs, kotlinx-serialization, and the
# Activity-callback bridges use reflection — renaming their classes/members silently breaks payments,
# so they are kept explicitly.

# ── Keep attributes needed for reflection, serialization, and readable crashes ──────────────────
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-keepattributes SourceFile, LineNumberTable
# Map obfuscated source file names so stack traces stay decodable with the mapping file.
-renamesourcefileattribute SourceFile

# ── kotlinx.serialization ───────────────────────────────────────────────────────────────────────
# (kotlinx-serialization ships consumer rules since 1.4, but these make the @Serializable protocol
# DTOs in core:protocol bullet-proof under fullMode R8.)
-keepclassmembers class **$$serializer { *; }
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.paymentslab.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.paymentslab.** {
    *** Companion;
    *** INSTANCE;
}

# ── Kotlin metadata / coroutines ────────────────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlin.Metadata { public <methods>; }

# ── Razorpay (WebView-bridge SDK; needs its classes + JS interface intact) ──────────────────────
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-optimizations !method/inlining/*
-dontwarn proguard.annotation.**

# ── Cashfree ────────────────────────────────────────────────────────────────────────────────────
-keep class com.cashfree.pg.** { *; }
-dontwarn com.cashfree.pg.**

# ── Stripe + Google Pay (ship consumer rules; silence optional transitive refs) ─────────────────
-dontwarn com.stripe.android.**
-dontwarn com.google.android.gms.**

# ── The Activity-level payment callbacks the SDKs invoke on MainActivity by name ────────────────
-keepclassmembers class com.paymentslab.app.MainActivity {
    public void onPayment*(...);
    public void onPaymentVerify(...);
    public void onPaymentFailure(...);
}

# ── WorkManager workers (constructed by class; keep the ctor) ───────────────────────────────────
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.paymentslab.app.work.** { *; }

# ── Ktor engines (optional transitive engines we don't ship) ────────────────────────────────────
-dontwarn io.ktor.**
-dontwarn org.slf4j.**

# ── Napier logging ──────────────────────────────────────────────────────────────────────────────
-dontwarn io.github.aakira.napier.**
