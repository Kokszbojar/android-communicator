//package com.example.frontend
//
//import kotlin.properties.ReadWriteProperty
//import kotlin.reflect.KProperty
//
//open class SmartDevice(val name: String, val category: String) {
//    var deviceStatus = "online"
//        protected set
//
//    open val deviceType = "unknown"
//
//    open fun printDeviceInfo() {
//        println("Device name: $name, category: $category, type: $deviceType")
//    }
//
//    open fun turnOn() {
//        // function body
//    }
//
//    open fun turnOff() {
//        // function body
//    }
//}
//
//class SmartTvDevice(deviceName: String, deviceCategory: String) :
//    SmartDevice(name = deviceName, category = deviceCategory) {
//
//    override val deviceType = "Smart TV"
//
//    private var speakerVolume by RangeRegulator(initialValue = 10, minValue = 0, maxValue = 100)
//
//    private var channelNumber by RangeRegulator(initialValue = 1, minValue = 0, maxValue = 200)
//
//    fun increaseSpeakerVolume() {
//        speakerVolume++
//        println("Speaker volume increased to $speakerVolume.")
//    }
//
//    fun decreaseSpeakerVolume() {
//        speakerVolume--
//        println("Speaker volume decreased to $speakerVolume.")
//    }
//
//    fun nextChannel() {
//        channelNumber++
//        println("Channel number increased to $channelNumber.")
//    }
//
//    fun previousChannel() {
//        channelNumber--
//        println("Channel number decreased to $channelNumber.")
//    }
//
//    override fun turnOn() {
//        deviceStatus = "on"
//        println(
//            "$name is turned on. Speaker volume is set to $speakerVolume and channel number is " +
//                    "set to $channelNumber."
//        )
//    }
//
//    override fun turnOff() {
//        deviceStatus = "off"
//        println("$name turned off")
//    }
//}
//
//class SmartLightDevice(deviceName: String, deviceCategory: String) :
//    SmartDevice(name = deviceName, category = deviceCategory) {
//
//    override val deviceType = "Smart Light"
//
//    private var brightnessLevel by RangeRegulator(initialValue = 0, minValue = 0, maxValue = 100)
//
//    fun increaseBrightness() {
//        brightnessLevel++
//        println("Brightness increased to $brightnessLevel.")
//    }
//
//    fun decreaseBrightness() {
//        brightnessLevel--
//        println("Brightness decreased to $brightnessLevel.")
//    }
//
//    override fun turnOn() {
//        deviceStatus = "on"
//        brightnessLevel = 2
//        println("$name turned on. The brightness level is $brightnessLevel.")
//    }
//
//    override fun turnOff() {
//        deviceStatus = "off"
//        brightnessLevel = 0
//        println("Smart Light turned off")
//    }
//}
//
//class SmartHome(
//    val smartTvDevice: SmartTvDevice,
//    val smartLightDevice: SmartLightDevice
//) {
//
//    fun turnOnTv() {
//        smartTvDevice.turnOn()
//    }
//
//    fun turnOffTv() {
//        smartTvDevice.turnOff()
//    }
//
//    fun increaseTvVolume() {
//        smartTvDevice.increaseSpeakerVolume()
//    }
//
//    fun changeTvChannelToNext() {
//        smartTvDevice.nextChannel()
//    }
//
//    fun turnOnLight() {
//        smartLightDevice.turnOn()
//    }
//
//    fun turnOffLight() {
//        smartLightDevice.turnOff()
//    }
//
//    fun increaseLightBrightness() {
//        smartLightDevice.increaseBrightness()
//    }
//
//    fun turnOffAllDevices() {
//        turnOffTv()
//        turnOffLight()
//    }
//
//}
//
//class RangeRegulator(
//    initialValue: Int,
//    private val minValue: Int,
//    private val maxValue: Int
//) : ReadWriteProperty<Any?, Int> {
//
//    var fieldData = initialValue
//
//    override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
//        return fieldData
//    }
//
//    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
//        if (value in minValue..maxValue) {
//            fieldData = value
//        }
//    }
//}
//
//fun main() {
//    var smartTvDevice: SmartDevice = SmartTvDevice("Android TV", "Entertainment")
//    smartTvDevice.printDeviceInfo()
//
//    smartTvDevice = SmartLightDevice("Google Light", "Utility")
//    smartTvDevice.turnOn()
//}
//
//class Artist(val firstName: String, val secondName: String, var age: Int, var sex: String) {
//}
//
//class Album(val name: String, val artist: Artist) {
//}
//
//class Song(val title: String, val text: String, val publishYear: Int, var playCount: Int, val album: Album) {
//
//    var popular = false
//
//    fun increase_count() {
//        playCount++
//        if (playCount >= 1000 && popular == false) {
//            popular = true
//        }
//        println("$playCount, $popular")
//    }
//
//    fun print_info() {
//        val firstName = album.artist.firstName
//        val secondName = album.artist.secondName
//        println("$title, performed by $firstName $secondName, was released in $publishYear.")
//    }
//}
//
//fun main() {
//    val me = Artist("Krystian", "Kuklicz", 24, "male")
//    val my_album = Album("Masno GnG", me)
//    val song = Song("COCO", "Ona patrzy w moje oczy", 2020, 998, my_album)
//    song.print_info()
//    song.increase_count()
//}