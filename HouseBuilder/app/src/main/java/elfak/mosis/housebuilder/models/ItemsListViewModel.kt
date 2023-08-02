package elfak.mosis.housebuilder.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ItemsListViewModel: ViewModel() {

    private val _longitude = MutableLiveData<String>()
    val longitude: LiveData<String> = _longitude

    private val _latitude = MutableLiveData<String>()
    val latitude: LiveData<String> = _latitude

    private val _flag = MutableLiveData<String>()
    val flag: LiveData<String> = _flag

    fun setLatLong(lat: String, lon: String){
        _latitude.value = lat
        _longitude.value = lon
    }

    fun setFlag(f: String){
        _flag.value = f
    }
}