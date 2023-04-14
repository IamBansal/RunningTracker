package com.example.runningtracker.adapters

import android.icu.util.Calendar
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.runningtracker.databinding.ItemRunBinding
import com.example.runningtracker.db.Run
import com.example.runningtracker.utils.TrackingUtility
import java.text.SimpleDateFormat
import java.util.*

class RunAdapter : RecyclerView.Adapter<RunAdapter.RunViewHolder>() {

    inner class RunViewHolder(private var binding: ItemRunBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @RequiresApi(Build.VERSION_CODES.N)
        fun bind(run: Run) {
            Glide.with(itemView).load(run.image).into(binding.ivRunImage)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = run.timeStamp
            }

            binding.tvDate.text =
                SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(calendar.time)

            "${run.avgSpeedInKMPH}km/h".also { binding.tvAvgSpeed.text = it }

            "${run.distanceInMeters / 1000f}km".also { binding.tvDistance.text = it }

            "${run.caloriesBurned}kcal".also { binding.tvCalories.text = it }

            binding.tvTime.text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)

        }

    }

    private val differCallback = object : DiffUtil.ItemCallback<Run>() {
        override fun areItemsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

    }

    val differ = AsyncListDiffer(this, differCallback)

    fun submitList(list: List<Run>) = differ.submitList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunViewHolder {
        return RunViewHolder(
            ItemRunBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onBindViewHolder(holder: RunViewHolder, position: Int) {
        val run = differ.currentList[position]
        holder.bind(run)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

}