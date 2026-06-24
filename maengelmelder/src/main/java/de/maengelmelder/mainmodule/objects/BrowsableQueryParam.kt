package de.maengelmelder.mainmodule.objects

class BrowsableQueryParam {
    public var forceLocation = false
    public var lat: Double = 0.0
    public var lon: Double = 0.0
    public var forceType = false
    public var category: Category? = null
    public var attributeValues: HashMap<String, Any?> = hashMapOf()
}