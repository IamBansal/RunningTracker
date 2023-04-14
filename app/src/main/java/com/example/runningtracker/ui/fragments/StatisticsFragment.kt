package com.example.runningtracker.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.runningtracker.R
import com.example.runningtracker.databinding.FragmentStatisticsBinding
import com.example.runningtracker.ui.viewmodels.StatisticsViewModel
import com.example.runningtracker.utils.CustomMarkerView
import com.example.runningtracker.utils.TrackingUtility
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.round

@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private val viewModel : StatisticsViewModel by viewModels()
    private lateinit var binding: FragmentStatisticsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()
        setupBarChart()
    }

    private fun setupBarChart(){
        binding.barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawLabels(false)
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
        }

        binding.barChart.axisLeft.apply {
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
        }

        binding.barChart.axisRight.apply {
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
        }

        binding.barChart.apply {
            description.text = "Avg Speed over time"
            legend.isEnabled = false
        }
    }

    private fun subscribeToObservers(){
        viewModel.totalTimeRun.observe(viewLifecycleOwner){
            it?.let {
                binding.tvTotalTime.text = TrackingUtility.getFormattedStopWatchTime(it)
            }
        }
        viewModel.totalDistance.observe(viewLifecycleOwner){ dist ->
            dist?.let { distance ->
                "${round((distance / 1000f) * 10f) / 10f} km".also { binding.tvTotalDistance.text = it }
            }
        }
        viewModel.totalAvgSpeed.observe(viewLifecycleOwner){ s ->
            s?.let { speed ->
                "${round(speed * 10f) / 10f} km".also { binding.tvAverageSpeed.text = it }
            }
        }
        viewModel.totalCaloriesBurned.observe(viewLifecycleOwner){ cal ->
            cal?.let { calories ->
                "$calories kcal".also { binding.tvTotalCalories.text = it }
            }
        }
        viewModel.runsSortedByDate.observe(viewLifecycleOwner){
            it?.let {
                val allAvgSpeeds = it.indices.map { i ->
                    BarEntry(i.toFloat(), it[i].avgSpeedInKMPH)
                }
                val barDataSet = BarDataSet(allAvgSpeeds, "Avg Speed Over Time").apply {
                    valueTextColor = Color.WHITE
                    color = ContextCompat.getColor(requireContext(), R.color.colorAccent)
                }
                binding.barChart.data = BarData(barDataSet)
                binding.barChart.marker = CustomMarkerView(it.reversed(), requireContext(), R.layout.marker_view)
                binding.barChart.invalidate()
            }
        }
    }

}