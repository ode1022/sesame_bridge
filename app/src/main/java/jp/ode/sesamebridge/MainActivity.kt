package jp.ode.sesamebridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import jp.ode.sesamebridge.databinding.ActivityMainBinding
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*


private lateinit var binding: ActivityMainBinding;

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.stopButton.setOnClickListener{
            val intent = Intent(this, MyService::class.java)
            stopService(intent)
        }

        binding.ipText.text = getIPAddress().map { "http://"+it+":5000" }.joinToString("\n")
        binding.settingButton.setOnClickListener{
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }
        setContentView(binding.root)

        createNotificationChannel()

        requestOverlayPermission()

        val intent = Intent(this, MyService::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startService(intent)
        } else {
            startForegroundService(intent)
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val nfcIdStr = prefs.getString("nfc_id", "")
        if (nfcIdStr == "") {
            val toast = Toast.makeText(this, "設定でNFCのUIDを設定してください", Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show()
            return
        }
    }

    private fun getIPAddress(): List<String> {
        var resultAddresses = mutableListOf<String>()
        val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val network: NetworkInterface = interfaces.nextElement()
            val addresses: Enumeration<InetAddress> = network.getInetAddresses()
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                // IPv4でなければ無視
                if (!(address is Inet4Address) ) {
                    continue
                }
                // ループバックアドレスは無視
                if ( address.isLoopbackAddress() ) {
                    continue
                }
                //resultAddresses.add(network.name)
                // セルラー接続(モバイルデータ)などは無視
                // https://stackoverflow.com/questions/33747787/android-networkinterface-what-are-the-meanings-of-names-of-networkinterface
                if ( network.name.startsWith("rmnet")) {
                    continue
                }

                resultAddresses.add(address.hostAddress)
            }
        }
        return resultAddresses
    }

    var OVERLAY_PERMISSION_REQUEST_CODE = 1
    /** Requests an overlay permission to the user if needed. */
    private fun requestOverlayPermission() {
        if (isOverlayGranted()) return
        val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
    }

    /** Checks if the overlay is permitted. */
    private fun isOverlayGranted() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(this)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!isOverlayGranted()) {
                finish()  // Cannot continue if not granted
            }
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    MyService.CHANNEL_ID,
                    "SesameBridge",
                    NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "SesameBridge通知"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}