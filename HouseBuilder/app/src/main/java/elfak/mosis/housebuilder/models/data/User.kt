package elfak.mosis.housebuilder.models.data

data class User(var username: String? = null,
                var password: String? = null,
                var firstname: String? = null,
                var lastname: String? = null,
                var phone: String? = null,
                var image: String? = null,
                var houseNumber: Int? = null,
                var points: Int? = null
) {
    fun addHouse(){
        houseNumber?.inc()
    }

    fun addPoint(item: Item){
        points = points!! + item.points!!
    }
}