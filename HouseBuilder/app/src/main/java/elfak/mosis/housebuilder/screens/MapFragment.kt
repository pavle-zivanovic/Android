package elfak.mosis.housebuilder.screens

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.models.LocationViewModel
import elfak.mosis.housebuilder.models.data.MarkerItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.time.LocalDateTime


class MapFragment : Fragment() {

    private lateinit var map: MapView
    private val locationViewModel: LocationViewModel by activityViewModels()
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private var auth : FirebaseAuth = Firebase.auth
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_map, container, false)

        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getAllMarkers()
        map = requireView().findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        var ctx: Context? = requireActivity().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences((ctx!!)))

        map.setMultiTouchControls(true)
        if(ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissionlauncher.launch(
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        else{
            setMyLoactionOverlay()
        }

        map.controller.setZoom(15.0)
        val startPoint = GeoPoint(43.3209, 21.8958)
        map.controller.setCenter(startPoint)

        val mRotationGestureOverlay = RotationGestureOverlay(context, map)
        mRotationGestureOverlay.setEnabled(true)
        map.setMultiTouchControls(true)
        map.getOverlays().add(mRotationGestureOverlay)

        val fab: FloatingActionButton = requireView().findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener{addItem()}

        val nameObserver = Observer<String> { newValue ->
            if(newValue != "no"){
                val marker = Marker(map)
                val point =
                    locationViewModel.latitude.value?.toDouble()
                        ?.let { locationViewModel.longitude.value?.toDouble()
                            ?.let { it1 -> GeoPoint(it, it1) } }
                marker.position = point
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map.overlays.add(marker)
                marker.title = locationViewModel.itemName.value

                var drawableImage: Int? = null
                when(locationViewModel.itemName.value){
                    "concrete" -> drawableImage = R.drawable.concrete
                    "brick" -> drawableImage = R.drawable.brick
                    "door" -> drawableImage = R.drawable.door
                    "window" -> drawableImage = R.drawable.window
                    "roof" -> drawableImage = R.drawable.roof
                    "chimney" -> drawableImage = R.drawable.chimney
                }
                val image: Drawable? = ResourcesCompat.getDrawable(resources, drawableImage!!, null)
                marker.image = image

                val m = MarkerItem(locationViewModel.itemName.value,
                    locationViewModel.longitude.value,
                    locationViewModel.latitude.value,
                    auth.currentUser?.email,
                    locationViewModel.points.value,
                    LocalDateTime.now().toString())

                db.collection("markers").add(m)
                locationViewModel.setItemName("no")
            }

        }
        locationViewModel.itemName.observe(viewLifecycleOwner, nameObserver)
    }

    private fun getAllMarkers(){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    db.collection("markers")
                        .get()
                        .await()
                }

                if(result != null){
                    for(d in result.documents){
                        val marker = Marker(map)
                        val point = GeoPoint(d.get("latitude").toString().toDouble(), d.get("longitude").toString().toDouble())
                        marker.position = point
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        map.overlays.add(marker)
                        marker.title = d.get("name").toString()

                        var drawableImage: Int? = null
                        when(locationViewModel.itemName.value){
                            "concrete" -> drawableImage = R.drawable.concrete
                            "brick" -> drawableImage = R.drawable.brick
                            "door" -> drawableImage = R.drawable.door
                            "window" -> drawableImage = R.drawable.window
                            "roof" -> drawableImage = R.drawable.roof
                            "chimney" -> drawableImage = R.drawable.chimney
                        }
                        val image: Drawable? = ResourcesCompat.getDrawable(resources, drawableImage!!, null)
                        marker.image = image
                    }
                }
            }
            catch (e: Exception) {
                Log.w("MARKER", "ERROR", e)
            }
        }
    }

    private fun addItem(){
        val loc = myLocationOverlay.myLocation
        if(loc != null){
            locationViewModel.setLocation(loc.longitude.toString(), loc.latitude.toString())
            findNavController().navigate(R.id.action_MapFragment_to_AddFragment)
        }
        else{
            Toast.makeText(view?.context, "Turn on location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setMyLoactionOverlay(){
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(activity), map)
        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)
    }

    private val requestPermissionlauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ){ isGranted: Boolean ->
            if(isGranted){
                setMyLoactionOverlay()
            }
        }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}