package elfak.mosis.housebuilder.models.data

import java.util.Date

data class MarkerItem(var name: String? = null,
                  var longitude: String? = null,
                  var latitude: String? = null,
                  var user: String? = null,
                  var points: Int? = null,
                  var dateCreated: String? = null
){
}