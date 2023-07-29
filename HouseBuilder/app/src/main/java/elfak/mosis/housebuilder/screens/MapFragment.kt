package elfak.mosis.housebuilder.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.helpers.CustomInfoWindow
import elfak.mosis.housebuilder.models.LocationViewModel
import elfak.mosis.housebuilder.models.data.Item
import elfak.mosis.housebuilder.models.data.MarkerItem
import elfak.mosis.housebuilder.models.data.User
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
    private lateinit var collectBtn: Button
    private var names: ArrayList<String> = arrayListOf("concrete", "brick", "door", "window", "roof", "chimney")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getAllMarkers()
        map = requireView().findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        val ctx: Context? = requireActivity().applicationContext
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

        val startPoint: GeoPoint = if (myLocationOverlay.myLocation == null)
            GeoPoint(43.3209, 21.8958)
        else
            GeoPoint(myLocationOverlay.myLocation.latitude, myLocationOverlay.myLocation.longitude)
        map.controller.setZoom(15.0)
        map.controller.setCenter(startPoint)

        val mRotationGestureOverlay = RotationGestureOverlay(context, map)
        mRotationGestureOverlay.isEnabled = true
        map.setMultiTouchControls(true)
        map.overlays.add(mRotationGestureOverlay)

        val fab: FloatingActionButton = requireView().findViewById(R.id.fab_add)
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
                marker.snippet = "${locationViewModel.points.value} points"
                marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.marker_icon, null)

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

                marker.infoWindow = CustomInfoWindow(map)

                collectBtn  = marker.infoWindow.view.findViewById(R.id.bubble_moreinfo)
                collectBtn.visibility = View.GONE

                val hash = GeoFireUtils.getGeoHashForLocation(
                    GeoLocation(
                        locationViewModel.latitude.value!!.toDouble(),
                        locationViewModel.longitude.value!!.toDouble()))

                val m = MarkerItem(locationViewModel.itemName.value,
                    locationViewModel.longitude.value,
                    locationViewModel.latitude.value,
                    auth.currentUser?.email,
                    locationViewModel.points.value,
                    LocalDateTime.now().toString(),
                    hash)

                db.collection("markers").add(m)
                    .addOnSuccessListener {
                        Toast.makeText(view.context, "Successfully posted!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener{
                        Toast.makeText(view.context, "Failed!", Toast.LENGTH_SHORT).show()
                    }
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
                        marker.snippet = d.get("points").toString() + " points"
                        marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.marker_icon, null)

                        var drawableImage: Int? = null
                        when(d.get("name").toString()){
                            "concrete" -> drawableImage = R.drawable.concrete
                            "brick" -> drawableImage = R.drawable.brick
                            "door" -> drawableImage = R.drawable.door
                            "window" -> drawableImage = R.drawable.window
                            "roof" -> drawableImage = R.drawable.roof
                            "chimney" -> drawableImage = R.drawable.chimney
                        }
                        val image: Drawable? = ResourcesCompat.getDrawable(resources, drawableImage!!, null)
                        marker.image = image

                        marker.infoWindow = CustomInfoWindow(map)

                        collectBtn  = marker.infoWindow.view.findViewById(R.id.bubble_moreinfo)
                        if(d.get("user").toString() == auth.currentUser?.email){
                            collectBtn.visibility = View.GONE
                        }
                        collectBtn.setOnClickListener{collectItem(d, marker)}
                    }
                }
            }
            catch (e: Exception) {
                Log.w("MARKER", "ERROR", e)
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun collectItem(item: DocumentSnapshot, marker: Marker){

        val myLoc =  GeoLocation(myLocationOverlay.myLocation.latitude, myLocationOverlay.myLocation.longitude)
        val itemLat = item.get("latitude").toString().toDouble()
        val itemLong = item.get("longitude").toString().toDouble()
        val itemLoc = GeoLocation(itemLat, itemLong)
        val radius = 3.0
        val distance = GeoFireUtils.getDistanceBetween(itemLoc, myLoc)

        if(distance <= radius){

            val itemId = item.id
            val itemPoints = item.get("points").toString().toInt()
            val item1 = Item(item.get("name").toString(),
                item.get("points").toString().toInt(),
                item.get("longitude").toString(),
                item.get("latitude").toString(),
                auth.currentUser?.uid)

            db.collection("markers").document(itemId).delete()
                .addOnSuccessListener { Log.d("MARKER", "Successfully deleted from markers") }
                .addOnFailureListener{ Log.d("MARKER", "Failed to delete in markers") }

            db.collection("collectedItems").add(item1)
                .addOnSuccessListener { Log.d("MARKER", "Success writing into collectedItems") }
                .addOnFailureListener{ Log.d("MARKER", "Fail writing into collectedItems") }

            var houseNumber = 0
            val userID: String = auth.currentUser?.uid ?: ""
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        db.collection("collectedItems")
                            .whereEqualTo("userID", userID)
                            .get()
                            .await()
                    }

                    var concreteNumber = 0
                    var brickNumber = 0
                    var roofNumber = 0
                    var doorNumber = 0
                    var windowNumber = 0
                    var chimneyNumber = 0

                    if(result != null){
                        for(it in result.documents){
                            if(it.get("name") == "concrete"){ concreteNumber += 1 }
                            else if(it.get("name") == "brick"){ brickNumber += 1 }
                            else if(it.get("name") == "roof"){ roofNumber += 1 }
                            else if(it.get("name") == "door"){ doorNumber += 1 }
                            else if(it.get("name") == "window"){ windowNumber += 1 }
                            else if(it.get("name") == "chimney"){ chimneyNumber += 1 }
                        }

                        if(concreteNumber == 12 && brickNumber == 8 && roofNumber == 4
                            && doorNumber == 1 && windowNumber == 4 && chimneyNumber == 1){
                            houseNumber = 1
                            var i: Item? = null
                            for(j in 1..30){
                                when(names.random()){
                                    "concrete" -> i =  Item("concrete",5, null, null, userID)
                                    "brick" -> i =  Item("brick",10, null, null, userID)
                                    "door" -> i =  Item("door",20, null, null, userID)
                                    "window" -> i =  Item("window",15, null, null, userID)
                                    "roof" -> i =  Item("roof",15, null, null, userID)
                                    "chimney" -> i =  Item("chimney",25, null, null, userID)
                                }
                                db.collection("receivedItems").add(i!!)
                            }

                            Toast.makeText(view?.context, "Congratulations, you've built a house!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch (e: Exception) {
                    Log.w("ITEM", "ERROR", e)
                }
            }

            var userPoints: Int? = 0
            var userHouseNumber: Int? = 0
            val database = Firebase.database("https://house-builder-7dd6e-default-rtdb.firebaseio.com/")
                .reference.child("users").child(userID)

                database.get()
                .addOnSuccessListener { result ->
                    val user = result.getValue<User>()
                    userPoints = user?.points?.plus(itemPoints)
                    userHouseNumber = user?.houseNumber?.plus(houseNumber)
                    database.child("points").setValue(userPoints)
                    database.child("houseNumber").setValue(userHouseNumber)
                }
                    .addOnFailureListener{
                    Log.d("USER", "Fail to update!") }

            Toast.makeText(view?.context, "Collected!", Toast.LENGTH_SHORT).show()
            marker.closeInfoWindow()
            map.overlays.remove(marker)
        }
        else{
            Toast.makeText(view?.context, "You need to get closer!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeAllMarkers() {
        val markers = map.overlays.filterIsInstance<Marker>().toMutableList()
        markers.forEach { m -> map.overlays.remove(m) }
        map.invalidate()
    }

    private fun addItem(){
        val loc = myLocationOverlay.myLocation
        if(loc != null){
            locationViewModel.setLocation(loc.longitude.toString(), loc.latitude.toString())
            AddFragment().show(childFragmentManager, "Add item dialog")
        }
        else{
            Toast.makeText(view?.context, "Turn on location!", Toast.LENGTH_SHORT).show()
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