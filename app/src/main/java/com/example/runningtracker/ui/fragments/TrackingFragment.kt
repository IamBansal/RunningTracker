package com.example.runningtracker.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.runningtracker.R
import com.example.runningtracker.databinding.FragmentTrackingBinding
import com.example.runningtracker.db.Run
import com.example.runningtracker.services.TrackingService
import com.example.runningtracker.services.Polyline
import com.example.runningtracker.ui.viewmodels.MainViewModel
import com.example.runningtracker.utils.Constants.ACTION_PAUSE_SERVICE
import com.example.runningtracker.utils.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runningtracker.utils.Constants.ACTION_STOP_SERVICE
import com.example.runningtracker.utils.Constants.MAP_ZOOM
import com.example.runningtracker.utils.Constants.POLYLINE_COLOR
import com.example.runningtracker.utils.Constants.POLYLINE_WIDTH
import com.example.runningtracker.utils.TrackingUtility
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import kotlin.math.round

@AndroidEntryPoint
class TrackingFragment : Fragment() {

    private val viewModel : MainViewModel by viewModels()
    private lateinit var binding: FragmentTrackingBinding
    private var map: GoogleMap? = null

    private var curTimeInMillis = 0L
    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    private var menu: Menu? = null
    private var weight = 80f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrackingBinding.inflate(inflater, container, false)

        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider{
            override fun onCreateMenu(menuu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.toolbar_tracking_menu, menuu)
                menu = menuu
                if (curTimeInMillis > 0L){
                    menu?.getItem(0)?.isVisible = true
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when(menuItem.itemId){
                    R.id.miCancelRun -> {
                        showCancelTrackingDialog()
                    }
                }
                return true
            }

        })

        return binding.root
    }

    private fun showCancelTrackingDialog(){
        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Cancel Run")
            .setIcon(R.drawable.ic_delete)
            .setMessage("Are you sure to cancel the run and delete all it's data?")
            .setPositiveButton("Yes"){_,_->
                stopRun()
            }
            .setNegativeButton("No"){ dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
            .show()
    }

    private fun stopRun() {
        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)

        binding.mapView.getMapAsync {
            map = it
            addAllPolyLines()
        }

        binding.btnToggleRun.setOnClickListener{
            toggleRun()
        }

        binding.btnFinishRun.setOnClickListener {
            zoomToSeeWholeTrack()
            endRunAndSaveToDb()
        }

        subscribeToObservers()
    }

    private fun subscribeToObservers(){
        TrackingService.isTracking.observe(viewLifecycleOwner){
            updateTracking(it)
        }

        TrackingService.pathPoints.observe(viewLifecycleOwner){
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        }

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner){
            curTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeInMillis, true)
            binding.tvTimer.text = formattedTime
        }
    }

    private fun toggleRun(){
        if (!isTracking){
            menu?.getItem(0)?.isVisible = true
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        } else {
            sendCommandToService(ACTION_PAUSE_SERVICE)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateTracking(isTracking: Boolean){
        this.isTracking = isTracking
        if (!isTracking){
            binding.btnToggleRun.text = "Start"
            binding.btnFinishRun.visibility = View.VISIBLE
        } else {
            binding.btnToggleRun.text = "Stop"
            menu?.getItem(0)?.isVisible = true
            binding.btnFinishRun.visibility = View.GONE
        }
    }

    private fun moveCameraToUser(){
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()){
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    private fun zoomToSeeWholeTrack(){
        val bounds = LatLngBounds.Builder()
        for (polyline in pathPoints){
            for (pos in polyline){
                bounds.include(pos)
            }
        }
        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                binding.mapView.width,
                binding.mapView.height,
                (binding.mapView.height * 0.05f).toInt()
            )
        )
    }

    private fun endRunAndSaveToDb(){
        map?.snapshot { bitmap ->
            var distanceInMeters = 0
            for (polyline in pathPoints){
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }
            val avgSpeed = round((distanceInMeters / 1000f) / (curTimeInMillis / 1000f / 60 / 60) * 10) / 10f
            val dateTimeStamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = ((distanceInMeters / 1000f) * weight).toInt()

            val run = Run(bitmap, dateTimeStamp, avgSpeed, distanceInMeters, curTimeInMillis, caloriesBurned)
            viewModel.insertRun(run)
            Snackbar.make(requireActivity().findViewById(R.id.rootView), "Run saved successfully", Snackbar.LENGTH_SHORT).show()
            stopRun()
        }
    }

    private fun addAllPolyLines(){
        for (polyline in pathPoints){
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyline(){
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1){
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun sendCommandToService(action: String) = Intent(requireContext(), TrackingService::class.java).also {
        it.action = action
        requireContext().startService(it)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

}