package jp.ode.sesamebridge

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.HttpServerRequestCallback


class MyService : Service() {
    companion object {
        const val CHANNEL_ID = "777"
    }

    lateinit var server : AsyncHttpServer

    override fun onCreate() {
        super.onCreate()

        server = AsyncHttpServer()
        server.get("/", HttpServerRequestCallback {request, response ->
            Log.d("test", "request start")
            var toggled = ""
            if (!request.query.get("toggled").isNullOrEmpty()) {
                toggled = """
<div class="position-fixed top-0 end-0 p-3" style="z-index: 5">
<div class="toast" role="alert" aria-live="assertive" aria-atomic="true" id="myToast">
  <div class="toast-header">
    <strong class="me-auto">通知</strong>
    <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="閉じる"></button>
  </div>
  <div class="toast-body">
    開閉しました！
  </div>
</div>
</div>
<script>
window.addEventListener('load', function() {
  document.querySelectorAll('.toast').forEach(function (toastNode) {
    var toast = new bootstrap.Toast(toastNode, {
      autohide: false
    })
    toast.show()
  })
})
</script>
"""
            }
            var body = """<html><head><meta charset="UTF-8" /><meta name="viewport" content="width=device-width, initial-scale=1.0" /><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta1/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-giJF6kkoqNQ00vy+HMDP7azOuL0xtbfIcaT9wjKHr8RbDVddVHyTfAAsrekwKmP1" crossorigin="anonymous"><script src="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta1/dist/js/bootstrap.min.js" integrity="sha384-pQQkAEnwaBkjpqZ8RU1fF1AKtTcHJwFl3pblpTlHXybJjHpMYo79HY3hIi4NKxyj" crossorigin="anonymous"></script></head><body>"""
            body += """${toggled}<form action="./" method="post"><div class="mx-auto" style="width: max-content;margin-top: 200px"><input type="submit" name="submit" value="Sesameトグル開閉" class="btn btn-primary"></div></form>"""
            // body += """<div class="container mx-auto"><form action="./" method="post" style="position: absolute; top: 50%; left: 50%; transform: transform: translateY(-50%) translateX(-50%);"><input type="submit" name="submit" value="Sesameトグル開閉">${toggled}</form></div>"""
            body += "</html>"
            response.send(body)
            Log.d("test", "request end")
        })
        server.post("/", HttpServerRequestCallback {request, response ->
            Log.d("test", "request start")
            sesameToggle()
            response.redirect("./?toggled=y")
            Log.d("test", "request end")
        })
        // listen on port 5000
        server.listen(5000)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, 0)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            //.setContentTitle("SesameBridge")
            //.setContentText("SesameBridgeです")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openIntent)
            .build()

        startForeground(9999, notification)

        return START_STICKY
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun stopService(name: Intent?): Boolean {
        return super.stopService(name)
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
        // https://qiita.com/niusounds/items/b61a080497357c5efbe9
        AsyncServer.getDefault().stop() // これを追加するとすぐ止まる
        stopSelf()
    }


    fun sesameToggle() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val nfcIdStr = prefs.getString("nfc_id", "")
        if (nfcIdStr == "") {
            return
        }

        val intent = Intent()
        intent.setAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
        val id = nfcIdStr.toString().split(":").map { it.toInt(16).toByte() }.toByteArray()

//            android-NFCタグスキャンを偽造してアプリをテストする-StackOverflow
//            https://stackoverflow.com/questions/34882171/testing-an-app-by-faking-nfc-tag-scan
//            How to mock a Android NFC Tag object for unit testing - Stack Overflow
//            https://stackoverflow.com/questions/30841803/how-to-mock-a-android-nfc-tag-object-for-unit-testing
//            android-コードからACTION_NDEF_DISCOVEREDインテントを作成する方法はありますか-スタックオーバーフロー
//            https://stackoverflow.com/questions/26712905/is-there-a-way-to-create-an-action-ndef-discovered-intent-from-code
//            android - How to simulate the tag touch from other application - Stack Overflow
//                    https://stackoverflow.com/questions/28046115/how-to-simulate-the-tag-touch-from-other-application
//            java-アプリ内開発者ツールのNFCタグをモックするにはどうすればよいですか？ - スタックオーバーフロー
//            https://stackoverflow.com/questions/55484913/how-do-i-mock-an-nfc-tag-for-an-in-app-developer-tool
        // targetSDK28にするとだめらしい。27までは大丈夫

        val ndefMessage = NdefMessage(
            NdefRecord.createTextRecord("ja", "candynfc")
        );

        val tagClass = Tag::class.java
        val createMockTagMethod = tagClass.getMethod(
            "createMockTag",
            ByteArray::class.java,
            IntArray::class.java,
            Array<Bundle>::class.java
        )

        val TECH_NFC_A = 1
        val TECH_NDEF = 6

        val EXTRA_NDEF_MSG = "ndefmsg"
        val EXTRA_NDEF_CARDSTATE = "ndefcardstate"
        val EXTRA_NDEF_TYPE = "ndeftype"

        val ndefBundle = Bundle()
        ndefBundle.putInt(EXTRA_NDEF_MSG, 48) // result for getMaxSize()
        ndefBundle.putInt(EXTRA_NDEF_CARDSTATE, 1) // 1: read-only, 2: read/write
        ndefBundle.putInt(
            EXTRA_NDEF_TYPE,
            2
        ) // 1: T1T, 2: T2T, 3: T3T, 4: T4T, 101: MF Classic, 102: ICODE
        // https://www.sony.co.jp/Products/felica/NFC/forum.html
        // を見ると
        // >Type 2はMIFARE Ultralight®（NXP社）
        // 2でいいみたい。
        ndefBundle.putParcelable(EXTRA_NDEF_MSG, ndefMessage)
        val mockTag = createMockTagMethod.invoke(
            null, id, intArrayOf(TECH_NFC_A, TECH_NDEF), arrayOf(null, ndefBundle)
        ) as Tag

        intent.putExtra(NfcAdapter.EXTRA_TAG, mockTag)
        intent.putExtra(NfcAdapter.EXTRA_ID, id)
        intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, arrayOf(ndefMessage))
        intent.setType("text/plain")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

}