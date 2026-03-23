package com.aniwond.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DownloadChoiceBottomSheet(
    private val staffelNr: String,
    private val onEpisode: () -> Unit,
    private val onSeason: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_download_choice, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnEpisode).setOnClickListener {
            dismissAllowingStateLoss()
            onEpisode()
        }

        view.findViewById<Button>(R.id.btnSeason).apply {
            text = getString(R.string.download_season) + " $staffelNr"
            setOnClickListener {
                dismissAllowingStateLoss()
                onSeason()
            }
        }
    }
}
