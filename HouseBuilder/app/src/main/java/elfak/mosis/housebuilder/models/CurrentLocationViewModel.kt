package elfak.mosis.housebuilder.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CurrentLocationViewModel: ViewModel() {

    private val _longitude = MutableLiveData<String>()
    val longitude: LiveData<String> = _longitude

    private val _latitude = MutableLiveData<String>()
    val latitude: LiveData<String> = _latitude

    fun setLocation(lon: String, lat: String){
        _longitude.value = lon
        _latitude.value = lat
    }
}