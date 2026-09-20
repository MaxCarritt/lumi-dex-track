package com.lumidex.track;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.DocumentsContract;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class SaveMonitor extends Service {
    private ScheduledExecutorService worker;
    private volatile boolean stopped;
    private byte[] previousRead;
    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("save", "Save monitoring", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        startForeground(1, new Notification.Builder(this, "save").setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Lumi Dex save monitoring").setContentText("Reading your selected save every 5 seconds. Pause in Settings.")
            .setContentIntent(open).setOngoing(true).build());
        worker = Executors.newSingleThreadScheduledExecutor();
        worker.scheduleWithFixedDelay(this::poll, 0, 5, TimeUnit.SECONDS);
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) { return START_NOT_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
    private void publishStatus(String status, String folder) {
        if (stopped) return;
        try {
            SharedPreferences prefs = getSharedPreferences("save", 0);
            JSONObject snapshot = new JSONObject(prefs.getString("snapshot", "{}"));
            snapshot.put("status", status).put("folder", folder).put("monitoring", true);
            prefs.edit().putString("snapshot", snapshot.toString()).apply();
        } catch (JSONException ignored) { }
    }
    private void poll() {
        String tree = getSharedPreferences("save", 0).getString("tree", "");
        if (tree.isEmpty()) return;
        String folder = "Selected save folder";
        try {
            Uri uri = Uri.parse(tree);
            String root = DocumentsContract.getTreeDocumentId(uri);
            folder = root;
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(uri, root);
            Uri save = null;
            try (Cursor cursor = getContentResolver().query(children, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
                if (cursor == null) throw new IOException("Cannot read folder. Choose it again.");
                while (cursor.moveToNext()) {
                    if ("SaveData.bin".equalsIgnoreCase(cursor.getString(1))) {
                        if (save != null) throw new IOException("Multiple save files found. Select one game's save folder.");
                        save = DocumentsContract.buildDocumentUriUsingTree(uri, cursor.getString(0));
                    }
                }
            }
            if (save == null) throw new IOException("No SaveData.bin here. Choose the folder directly containing the game's save.");
            byte[] bytes;
            try (InputStream input = getContentResolver().openInputStream(save); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (input == null) throw new IOException("Cannot open save.");
                byte[] buffer = new byte[16384]; int count;
                while ((count = input.read(buffer)) != -1) {
                    if (output.size() + count > 2_000_000) throw new IOException("Unsupported save size.");
                    output.write(buffer, 0, count);
                }
                bytes = output.toByteArray();
            }
            SaveParser.Result parsed = SaveParser.parseResult(bytes);
            if (!Arrays.equals(previousRead, bytes)) {
                previousRead = bytes;
                publishStatus("Waiting for a stable save (two matching reads)", folder);
                return;
            }
            String status = SaveParser.isLuminescent(bytes) ? "Connected · Luminescent species synced; individual forms stay manual" : "Connected · BDSP species synced; individual forms stay manual";
            if (SaveParser.isLuminescent(bytes) && !SaveParser.hasValidChecksum(bytes)) status += " · stable read, checksum unverified";
            publishResult(parsed, status, folder);
        } catch (Exception e) {
            previousRead = null;
            publishStatus(e instanceof SecurityException ? "Folder permission lost. Choose the save folder again." : Objects.toString(e.getMessage(), "Unable to read save; retrying."), folder);
        }
    }

    private void publishResult(SaveParser.Result parsed, String status, String folder) {
        try {
            SharedPreferences prefs = getSharedPreferences("save", 0);
            JSONObject snapshot = new JSONObject(prefs.getString("snapshot", "{}"));
            snapshot.put("caught", new JSONArray(parsed.species));
            JSONArray forms = new JSONArray();
            for (int[] pair : parsed.forms) forms.put(new JSONArray(pair));
            snapshot.put("forms", forms).put("status", status).put("folder", folder).put("monitoring", true);
            if (success) snapshot.put("lastSync", System.currentTimeMillis());
            prefs.edit().putString("snapshot", snapshot.toString()).apply();
        } catch (JSONException ignored) { }
    }
    @Override public void onDestroy() {
        stopped = true;
        if (worker != null) worker.shutdownNow();
        try {
            SharedPreferences prefs = getSharedPreferences("save", 0);
            JSONObject snapshot = new JSONObject(prefs.getString("snapshot", "{}"));
            snapshot.put("monitoring", false).put("status", "Monitoring paused; last synced catches remain locked");
            prefs.edit().putString("snapshot", snapshot.toString()).apply();
        } catch (JSONException ignored) { }
        super.onDestroy();
    }
}
