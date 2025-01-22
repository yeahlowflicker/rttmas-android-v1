package com.mwnl.rttmas_android.core

import android.widget.ImageView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView

class GUI(
    var imageViewDetection:     ImageView,
    var switchDetectionStatus:  MaterialSwitch,
    var textDetectionStatus:    MaterialTextView,
    var textGpsLatitude:        MaterialTextView,
    var textGpsLongitude:       MaterialTextView,
    var textSpeed:              MaterialTextView,
)