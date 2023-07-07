package elfak.mosis.housebuilder.helpers

import android.util.Log
import android.widget.Button
import elfak.mosis.housebuilder.R
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow

class CustomInfoWindow(mapView: MapView) : MarkerInfoWindow(R.layout.bubble, mapView) {

    private var btn: Button? = null

    init {
        btn = mapView.findViewById(R.id.bubble_moreinfo)
    }

    override fun onOpen(item: Any?) {
        super.onOpen(item)
        Log.d("BUTTON", btn.toString())
    }
}