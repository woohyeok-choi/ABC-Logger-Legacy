package kaist.iclab.abc.data.entities


import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abc.data.types.DeviceEventType
import kaist.iclab.abc.data.types.DeviceEventTypeConverter

/**
 * It represents Android'transitionScanClient system-wide triggerEvents that typically the OS sends broadcasts
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property type an device event (e.g., headset un/plugged, turn on/off, screen on/off, etc.)
 */

@Entity
data class DeviceEventEntity (
    @Convert(converter = DeviceEventTypeConverter::class, dbType = String::class)
    val type: DeviceEventType = DeviceEventType.UNDEFINED
) : Base()
