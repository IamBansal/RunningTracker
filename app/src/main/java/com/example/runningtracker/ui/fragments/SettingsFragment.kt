package com.example.runningtracker.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.runningtracker.R
import com.example.runningtracker.databinding.FragmentSettingsBinding
import com.example.runningtracker.utils.Constants.KEY_FIRST_TIME_TOGGLE
import com.example.runningtracker.utils.Constants.KEY_NAME
import com.example.runningtracker.utils.Constants.KEY_WEIGHT
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject
    lateinit var sharedPref: SharedPreferences

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadFieldsFromSharedPref()

        binding.btnApplyChanges.setOnClickListener {
            if (applyChangesToSharedPref()){
                Snackbar.make(requireView(), "Saved changes successfully", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(requireView(), "Please enter all fields correctly", Snackbar.LENGTH_SHORT).show()
            }
        }

    }

    private fun loadFieldsFromSharedPref() {
        binding.etName.setText(sharedPref.getString(KEY_NAME, ""))
        binding.etWeight.setText(sharedPref.getFloat(KEY_WEIGHT, 80f).toString())
    }

    private fun applyChangesToSharedPref() : Boolean {

        val name = binding.etName.text.toString()
        val weight = binding.etWeight.text.toString()

        if (name.isEmpty() || weight.isEmpty()) return false

        sharedPref.edit()
            .putString(KEY_NAME, name)
            .putFloat(KEY_WEIGHT, weight.toFloat())
            .putBoolean(KEY_FIRST_TIME_TOGGLE, false)
            .apply()

        "Let's go, $name!".also { requireActivity().findViewById<MaterialTextView>(R.id.tvToolbarTitle).text = it }
        return true

    }

}