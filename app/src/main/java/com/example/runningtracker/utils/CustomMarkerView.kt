package com.example.runningtracker.utils

import android.annotation.SuppressLint
import android.content.Context
import android.icu.util.Calendar
import android.os.Build
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.runningtracker.R
import com.example.runningtracker.db.Run
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("ViewConstructor")
class CustomMarkerView(
    private val runs: List<Run>,
    context: Context,
    layoutId: Int
) : MarkerView(context, layoutId) {

    override fun getOffset(): MPPointF {
        return MPPointF(-width / 2f, -height.toFloat())
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        super.refreshContent(e, highlight)
        if (e == null) return

        val curRunId = e.x.toInt()
        val run = runs[curRunId]

        val calendar = Calendar.getInstance().apply {
            timeInMillis = run.timeStamp
        }

        findViewById<TextView>(R.id.tvDate).text =
            SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(calendar.time)

        "${run.avgSpeedInKMPH}km/h".also { findViewById<TextView>(R.id.tvAvgSpeed).text = it }

        "${run.distanceInMeters / 1000f}km".also { findViewById<TextView>(R.id.tvDistance).text = it }

        "${run.caloriesBurned}kcal".also { findViewById<TextView>(R.id.tvCaloriesBurned).text = it }

        findViewById<TextView>(R.id.tvDuration).text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)

    }

}