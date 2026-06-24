package de.maengelmelder.mainmodule.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.adapters.LogsAdapter
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.databinding.MmActivityLogsBinding
import de.maengelmelder.mainmodule.objects.Log
import java.util.*
import kotlin.Comparator

class LogsActivity : AppCompatActivity(){

    private var mDB: MMDB? = null
    private lateinit var mBinding: MmActivityLogsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = MmActivityLogsBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mDB = MMDB.instance(this)
        refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_logs, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.remove_logs -> {
                mDB?.truncate(mDB?.constants?.TBL_LOGS?: "")
                refresh()
            }
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refresh() {
        val logs = mDB?.getLogs()

        mBinding.noLogs.visibility = if (logs?.isEmpty() == true) View.VISIBLE else View.GONE

        if (logs?.isNotEmpty() == true) {
            Collections.sort(logs, TimestampComparator)
            val adapter = LogsAdapter(this, logs = logs.toTypedArray())
            mBinding.logslist.adapter = adapter
        }
    }

    object TimestampComparator : Comparator<Log> {
        override fun compare(o1: Log?, o2: Log?): Int = when {
            (o1?.timestamp ?: 0) < (o2?.timestamp ?: 0) -> 1
            (o1?.timestamp ?: 0) > (o2?.timestamp ?: 0) -> -1
            else -> 0
        }
    }

}