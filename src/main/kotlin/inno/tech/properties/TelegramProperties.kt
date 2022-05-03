package inno.tech.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Конфигурации подключения к Telegram Bot API.
 * @param name имя бота
 * @param token токен
 * @param adminId идентификатор администратора
 */
@ConfigurationProperties("telegram")
@ConstructorBinding
data class TelegramProperties(
    val name: String,
    val token: String,
    val adminId: Long,
)
