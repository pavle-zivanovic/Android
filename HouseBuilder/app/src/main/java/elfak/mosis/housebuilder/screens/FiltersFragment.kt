package elfak.mosis.housebuilder.screens

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.housebuilder.R
import elfak.mosis.housebuilder.models.FilterItemsViewModel
import elfak.mosis.housebuilder.models.LocationViewModel
import elfak.mosis.housebuilder.models.data.MarkerItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FiltersFragment : Fragment() {

    private var filter = ""
    private var value = ""
    private var valuesCB : ArrayList<Int> = arrayListOf(0, 0, 0, 0, 0)
    private var valuesDate : ArrayList<String> = arrayListOf("", "")
    private lateinit var etRadius: EditText
    private lateinit var etAuthor: EditText
    private lateinit var spinnerType: AutoCompleteTextView
    private lateinit var txtInputLayoutSpinner: TextInputLayout
    private lateinit var fromDateBtn: Button
    private lateinit var toDateBtn: Button
    private lateinit var filterBtn: Button
    private lateinit var cb5: CheckBox
    private lateinit var cb10: CheckBox
    private lateinit var cb15: CheckBox
    private lateinit var cb20: CheckBox
    private lateinit var cb25: CheckBox
    private lateinit var etFromDate: EditText
    private lateinit var etToDate: EditText
    private var names: ArrayList<String> = arrayListOf("concrete", "brick", "door", "window", "roof", "chimney")
    private var itemName: String = ""
    private var dateFlag = 0
    private var checkedFlag = 0
    private var db = Firebase.firestore
    private var markers : ArrayList<MarkerItem> = arrayListOf()
    private val filterViewModel: FilterItemsViewModel by activityViewModels()
    private val locationViewModel: LocationViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_filters, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etRadius = requireView().findViewById(R.id.editText_search_radius)
        etAuthor = requireView().findViewById(R.id.editText_search_author)
        spinnerType = requireView().findViewById(R.id.spinner)
        txtInputLayoutSpinner = requireView().findViewById(R.id.textInputLayoutSpinner)
        fromDateBtn = requireView().findViewById(R.id.button_from_date)
        toDateBtn = requireView().findViewById(R.id.button_to_date)
        filterBtn = requireView().findViewById(R.id.button_filter)
        cb5 = requireView().findViewById(R.id.cbx5)
        cb10 = requireView().findViewById(R.id.cbx10)
        cb15 = requireView().findViewById(R.id.cbx15)
        cb20 = requireView().findViewById(R.id.cbx20)
        cb25 = requireView().findViewById(R.id.cbx25)
        etFromDate = requireView().findViewById(R.id.editText_from_date)
        etToDate = requireView().findViewById(R.id.editText_to_date)

        etFromDate.isEnabled = false
        etToDate.isEnabled = false
        filterBtn.isEnabled = false
        enableDisableControls(false,false, false,false,
            false, false, false, false, false, false)

        val adapter = ArrayAdapter(requireView().context, android.R.layout.simple_spinner_dropdown_item, names as List<Any?>)
        spinnerType.setAdapter(adapter)
        spinnerType.setOnItemClickListener { adapterview, view, i, l ->
            itemName = adapterview.getItemAtPosition(i).toString()
            when(itemName){
                "concrete" -> txtInputLayoutSpinner.setStartIconDrawable(R.drawable.baseline_concrete)
                "brick" -> txtInputLayoutSpinner.setStartIconDrawable(R.drawable.baseline_brick)
                "door" -> txtInputLayoutSpinner.setStartIconDrawable(R.drawable.baseline_door)
                "window" ->txtInputLayoutSpinner.setStartIconDrawable(R.drawable.baseline_window)
                "roof" -> txtInputLayoutSpinner.setStartIconDrawable(R.drawable.baseline_roof)
                "chimney" -> txtInputLayoutSpinner.setStartIconDrawable(R.drawable.baseline_chimney)
            }
        }

        val rbGroup: RadioGroup = requireView().findViewById(R.id.radiogroupMap)
        rbGroup.setOnCheckedChangeListener{ _, selected ->
            when(selected){
                R.id.rbAuthor -> {
                    filter = "author"
                    enableDisableControls(false,false, false,false,
                        false, false, false, false, true, false)
                    clearControls()
                    filterBtn.isEnabled = false
                    dateFlag = 0
                }
                R.id.rbType -> {
                    filter = "type"
                    enableDisableControls(false,false, false,false,
                        false, false, false, false, false, true)
                    clearControls()
                    filterBtn.isEnabled = false
                    dateFlag = 0
                }
                R.id.rbRadius -> {
                    filter = "radius"
                    enableDisableControls(true,false, false,false,
                        false, false, false, false, false, false)
                    clearControls()
                    filterBtn.isEnabled = false
                    dateFlag = 0
                }
                R.id.rbPoints -> {
                    filter = "points"
                    enableDisableControls(false,false, false,true,
                        true, true, true, true, false, false)
                    clearControls()
                    filterBtn.isEnabled = false
                    dateFlag = 0
                }
                R.id.rbDate -> {
                    filter = "date"
                    enableDisableControls(false,true, true,false,
                        false, false, false, false, false, false)
                    clearControls()
                    filterBtn.isEnabled = false
                }
            }
        }

        fromDateBtn.setOnClickListener{openDateDialog(true)}
        toDateBtn.setOnClickListener {openDateDialog(false)}
        addListener(etAuthor)
        addListener(etRadius)
        addListener(spinnerType)
        addCheckedListener(cb5)
        addCheckedListener(cb10)
        addCheckedListener(cb15)
        addCheckedListener(cb20)
        addCheckedListener(cb25)
        addListenerDate(etFromDate)
        addListenerDate(etToDate)
        filterBtn.setOnClickListener {filterItems()}
    }

    private fun addListenerDate(o: EditText){
        o.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(text: Editable?) {
                if(text.toString() != ""){
                    dateFlag+=1
                    when(o){
                        etFromDate -> valuesDate[0] = text.toString()
                        etToDate -> valuesDate[1] = text.toString()
                    }
                    if(dateFlag == 2){
                        filterBtn.isEnabled = true
                    }
                }
            }
        })
    }

    private fun addCheckedListener(c: CheckBox){
        c.setOnCheckedChangeListener { _, checked ->
            if(checked){
                checkedFlag+=1
                when(c.text){
                    "5" -> valuesCB[0] = 5
                    "10" -> valuesCB[1] = 10
                    "15" -> valuesCB[2] = 15
                    "20" -> valuesCB[3] = 20
                    "25" -> valuesCB[4] = 25
                }
            }
            else{
                checkedFlag-=1
                when(c.text){
                    "5" -> valuesCB[0] = 0
                    "10" -> valuesCB[1] = 0
                    "15" -> valuesCB[2] = 0
                    "20" -> valuesCB[3] = 0
                    "25" -> valuesCB[4] = 0
                }
            }
            filterBtn.isEnabled = checkedFlag > 0
        }
    }

    private fun addListener(o: EditText){
        o.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(text: Editable?) {
                filterBtn.isEnabled = o.text.isNotEmpty()
                value = text.toString()
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openDateDialog(from: Boolean){
        val currentDate = LocalDateTime.now()
        if(from){
            val dateListener = OnDateSetListener { _, year, month, day ->
                val m = month+1
                val d = "$year-$m-$day"
                etFromDate.text = d.toEditable()
            }
            val dialog = DatePickerDialog(requireView().context, R.style.DialogTheme, dateListener,
                currentDate.year, currentDate.monthValue-1, currentDate.dayOfMonth)
            dialog.show()
        }
        else{
            val dateListener = OnDateSetListener { _, year, month, day ->
                val m = month+1
                val d = "$year-$m-$day"
                etToDate.text = d.toEditable()
            }
            val dialog = DatePickerDialog(requireView().context, R.style.DialogTheme, dateListener,
                currentDate.year, currentDate.monthValue-1, currentDate.dayOfMonth)
            dialog.show()
        }
    }

    private fun enableDisableControls(i1: Boolean, i2: Boolean, i3: Boolean, i4: Boolean, i5: Boolean,
                                      i6: Boolean, i7: Boolean, i8: Boolean, i9: Boolean, i10: Boolean){
        etRadius.isEnabled = i1
        fromDateBtn.isEnabled = i2
        toDateBtn.isEnabled = i3
        cb5.isEnabled = i4
        cb10.isEnabled = i5
        cb15.isEnabled = i6
        cb20.isEnabled = i7
        cb25.isEnabled = i8
        etAuthor.isEnabled = i9
        txtInputLayoutSpinner.isEnabled = i10
        //spinnerType.isEnabled = false
    }

    private fun clearControls(){
        etRadius.text = "".toEditable()
        etAuthor.text = "".toEditable()
        spinnerType.text = "".toEditable()
        cb5.isChecked = false
        cb10.isChecked = false
        cb15.isChecked = false
        cb20.isChecked = false
        cb25.isChecked = false
        etFromDate.text = "".toEditable()
        etToDate.text = "".toEditable()
        txtInputLayoutSpinner.isStartIconVisible = false
    }

    private fun filterItems(){
        when(filter){
            "author" -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            db.collection("markers")
                                .whereEqualTo("user", "$value@gmail.com")
                                .get()
                                .await()
                        }

                        if(result != null){
                            for(d in result.documents){
                               val marker = MarkerItem(d.get("name").toString(), d.get("longitude").toString(),
                                   d.get("latitude").toString(), d.get("user").toString(), d.get("points").toString().toInt(),
                               d.get("dateCreated").toString(), d.get("hash").toString(), d.id)
                                markers.add(marker)
                            }
                            filterViewModel.setItems(markers)
                            filterViewModel.setFlag("no")
                            markers.clear()
                            findNavController().navigate(R.id.action_FiltersFragment_to_MapFragment)
                        }
                    } catch (e: Exception) {
                        Log.w("ITEM", "ERROR", e)
                    }
                }
            }
            "type" -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            db.collection("markers")
                                .whereEqualTo("name", value)
                                .get()
                                .await()
                        }

                        if(result != null){
                            for(d in result.documents){
                                val marker = MarkerItem(d.get("name").toString(), d.get("longitude").toString(),
                                    d.get("latitude").toString(), d.get("user").toString(), d.get("points").toString().toInt(),
                                    d.get("dateCreated").toString(), d.get("hash").toString(), d.id)
                                markers.add(marker)
                            }
                            filterViewModel.setItems(markers)
                            filterViewModel.setFlag("no")
                            markers.clear()
                            findNavController().navigate(R.id.action_FiltersFragment_to_MapFragment)
                        }
                    } catch (e: Exception) {
                        Log.w("ITEM", "ERROR", e)
                    }
                }
            }
            "radius" -> {
                val myLoc =  GeoLocation(
                    locationViewModel.latitude.value!!.toDouble(),
                    locationViewModel.longitude.value!!.toDouble())
                val radius = value.toDouble()

                val bounds = GeoFireUtils.getGeoHashQueryBounds(myLoc, radius)
                val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
                for (b in bounds) {
                    val q = db.collection("markers")
                        .orderBy("hash")
                        .startAt(b.startHash)
                        .endAt(b.endHash)
                    tasks.add(q.get())
                }

                Tasks.whenAllComplete(tasks)
                    .addOnCompleteListener {
                        val matchingDocs: MutableList<DocumentSnapshot> = ArrayList()
                        for (task in tasks) {
                            val snap = task.result
                            for (doc in snap!!.documents) {
                                val lat = doc.get("latitude").toString().toDouble()
                                val lng = doc.get("longitude").toString().toDouble()

                                val docLocation = GeoLocation(lat, lng)
                                val distanceInM =
                                    GeoFireUtils.getDistanceBetween(docLocation, myLoc)
                                if (distanceInM <= radius) {
                                    matchingDocs.add(doc)
                                }
                            }
                        }

                        for(d in matchingDocs){
                            val marker = MarkerItem(d.get("name").toString(), d.get("longitude").toString(),
                                d.get("latitude").toString(), d.get("user").toString(), d.get("points").toString().toInt(),
                                d.get("dateCreated").toString(), d.get("hash").toString(), d.id)
                            markers.add(marker)
                        }
                        filterViewModel.setItems(markers)
                        filterViewModel.setFlag("no")
                        markers.clear()
                        //Log.d("AUTHOR", filterViewModel.items.value.toString())
                        findNavController().navigate(R.id.action_FiltersFragment_to_MapFragment)
                    }
            }
            "points" -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            db.collection("markers")
                                .whereIn("points", valuesCB)
                                .get()
                                .await()
                        }

                        if(result != null){
                            for(d in result.documents){
                                val marker = MarkerItem(d.get("name").toString(), d.get("longitude").toString(),
                                    d.get("latitude").toString(), d.get("user").toString(), d.get("points").toString().toInt(),
                                    d.get("dateCreated").toString(), d.get("hash").toString(), d.id)
                                markers.add(marker)
                            }
                            filterViewModel.setItems(markers)
                            filterViewModel.setFlag("no")
                            markers.clear()
                            findNavController().navigate(R.id.action_FiltersFragment_to_MapFragment)
                        }
                    } catch (e: Exception) {
                        Log.w("ITEM", "ERROR", e)
                    }
                }
            }
            "date" -> {
                val dates = ArrayList<String>()
                val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                var date1: Date? = null
                var date2:Date? = null
                try
                {
                    date1 = input.parse(valuesDate[0])
                    date2 = input.parse(valuesDate[1])
                }
                catch (e: ParseException) {
                    e.printStackTrace()
                }
                val cal1 = Calendar.getInstance()
                if (date1 != null) {
                    cal1.time = date1
                }
                val cal2 = Calendar.getInstance()
                if (date2 != null) {
                    cal2.time = date2
                }
                while (!cal1.after(cal2))
                {
                    val output = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    dates.add(output.format(cal1.time))
                    cal1.add(Calendar.DATE, 1)
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            db.collection("markers")
                                .whereIn("dateCreated", dates)
                                .get()
                                .await()
                        }

                        if(result != null){
                            for(d in result.documents){
                                val marker = MarkerItem(d.get("name").toString(), d.get("longitude").toString(),
                                    d.get("latitude").toString(), d.get("user").toString(), d.get("points").toString().toInt(),
                                    d.get("dateCreated").toString(), d.get("hash").toString(), d.id)
                                markers.add(marker)
                            }
                            filterViewModel.setItems(markers)
                            filterViewModel.setFlag("no")
                            markers.clear()
                            findNavController().navigate(R.id.action_FiltersFragment_to_MapFragment)
                        }
                    } catch (e: Exception) {
                        Log.w("ITEM", "ERROR", e)
                    }
                }
            }
        }
    }

    fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)
}