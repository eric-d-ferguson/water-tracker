# R8 rules for release builds.

# WorkManager creates input mergers by reflection through their no-argument constructor.
# R8 was removing OverwritingInputMerger's, which stopped Glance (it draws widgets via
# WorkManager) from ever rendering: the widget stayed on its loading spinner.
-keep class * extends androidx.work.InputMerger {
    public <init>();
}

# Workers are also created by reflection, with this constructor.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Glance creates widget button handlers (actionRunCallback) by reflection through their
# no-argument constructor. Without this, R8 keeps the class but removes the constructor and
# every widget button tap fails.
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}
