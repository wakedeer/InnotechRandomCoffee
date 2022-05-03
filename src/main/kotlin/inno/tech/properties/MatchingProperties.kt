package inno.tech.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@ConfigurationProperties("matching.fast")
@ConstructorBinding
class MatchingProperties(val from: DayTimeOfWeek, val to: DayTimeOfWeek)

class DayTimeOfWeek(val day: DayOfWeek, val time: LocalTime)

@Component
@ConfigurationPropertiesBinding
class LocalTimeConverter : Converter<String, LocalTime> {
    override fun convert(source: String): LocalTime = LocalTime.parse(source, DateTimeFormatter.ofPattern("HH:mm:ss"))
}
