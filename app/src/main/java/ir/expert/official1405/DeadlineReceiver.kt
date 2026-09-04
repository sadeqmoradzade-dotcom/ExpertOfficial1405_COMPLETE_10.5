package ir.expert.official1405

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class DeadlineReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        val title=intent.getStringExtra("title")?:"مهلت کارشناسی"
        val date=intent.getStringExtra("date")?:""
        val nm=context.getSystemService(NotificationManager::class.java)
        if(android.os.Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(NotificationChannel("deadlines","یادآوری مهلت‌ها",NotificationManager.IMPORTANCE_HIGH))
        val n=NotificationCompat.Builder(context,"deadlines").setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("یادآوری مهلت کارشناسی").setContentText("$title — سررسید: $date").setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        nm.notify((System.currentTimeMillis()%100000).toInt(),n)
    }
}
