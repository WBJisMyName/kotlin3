package com.transcend.otg.data

import androidx.databinding.ObservableInt

data class EULAOberableField (
    var buttonVisibility : Int,//GONE = 8, Invisible = 4, visible = 0
    var progressVisibility : ObservableInt,
    var url : String
)
