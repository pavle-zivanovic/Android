package elfak.mosis.housebuilder.models.data

import android.graphics.drawable.Drawable

data class ListItems(var name: String? = null,
                     var author: String? = null,
                     var points: String? = null,
                     var image: Drawable? = null,
                     var date: String? = null,
                     var latitude: String? = null,
                     var longitude: String? = null
) {
}