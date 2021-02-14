package jp.ode.sesamebridge

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.tech.NfcF
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var mNfcAdapter: NfcAdapter
    private var pendingIntent: PendingIntent? = null
    private var intentFilters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)



        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        // 受け取るIntentを指定
        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED))

        // 反応するタグの種類を指定
        techLists = arrayOf(
            arrayOf(android.nfc.tech.Ndef::class.java.name),
            arrayOf(android.nfc.tech.NdefFormatable::class.java.name),
            arrayOf(NfcF::class.java.name)
        )

        mNfcAdapter = NfcAdapter.getDefaultAdapter(applicationContext)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()

        // NFCタグの検出を有効化
        mNfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists)
    }

    override fun onPause() {
        super.onPause()

        mNfcAdapter?.disableForegroundDispatch(this)
    }

    fun ByteArray.toHexString()=joinToString(":"){"%02x".format(it)}

    /**
     * NFCタグの検出時に呼ばれる
     */
    override fun onNewIntent(intent: Intent) {
        val f = supportFragmentManager.findFragmentById(R.id.settings) as SettingsFragment

        // タグのIDを取得
        val tagId: ByteArray = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID) ?: return
        val editTextPreference: EditTextPreference = f.findPreference("nfc_id")!!
        editTextPreference.text = tagId.toHexString()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val editTextPreference: EditTextPreference? = findPreference("nfc_id")
            editTextPreference?.setOnPreferenceChangeListener { preference, newValue ->
                if (newValue.toString().isEmpty()) {
                    return@setOnPreferenceChangeListener true
                }
                //logError( "Pref " + preference.key + " changed to " + newValue.toString())
                try {
                    val byteArray =
                            newValue.toString().split(":").map { it -> it.toInt(16).toByte() }
                                    .toByteArray()
                    if (byteArray.size == 7) {
                        true
                    } else {
                        AlertDialog.Builder(this.requireContext())
                                .setTitle("エラー")
                                .setMessage("7byte分入力してください. 入力数:${byteArray.size}")
                                .setPositiveButton("OK") { dialog, which -> }
                                .show()
                        false
                    }
                } catch (e: Exception) {
                    AlertDialog.Builder(this.requireContext())
                            .setTitle("エラー")
                            .setMessage("入力形式が正しくありません")
                            .setPositiveButton("OK") { dialog, which -> }
                            .show()
                    false
                }
            }
        }
    }
}