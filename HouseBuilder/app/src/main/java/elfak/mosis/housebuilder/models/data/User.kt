package elfak.mosis.housebuilder.models.data

data class User(var username: String? = null,
                var password: String? = null,
                var firstname: String? = null,
                var lastname: String? = null,
                var phone: String? = null,
                var image: String? = null,
                var houseNumber: Int? = null,
                var points: Int? = null,
                var items: Int? = null,
                var concreteNumber: Int? = null,
                var brickNumber: Int? = null,
                var roofNumber: Int? = null,
                var doorNumber: Int? = null,
                var windowNumber: Int? = null,
                var chimneyNumber: Int? = null
) {
}