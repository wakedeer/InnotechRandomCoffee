package inno.tech.handler.admin

import inno.tech.TelegramProperties
import inno.tech.handler.Handler
import inno.tech.model.User
import inno.tech.service.subscription.SubscriptionService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

/**
 * Обработчик ручной рассылки вопросов о замене партнёра.
 *
 * @param telegramProperties конфигурации подключения к Telegram Bot API
 * @param subscriptionService сервис отправки уведомлений
 */
@Component
@Profile(value = ["dev", "manual"])
class ManualRematchHandler(
    private val telegramProperties: TelegramProperties,
    private val subscriptionService: SubscriptionService,
) : Handler {

    override fun accept(command: String, user: User?): Boolean {
        return telegramProperties.adminId == user?.userId && COMMAND == command
    }

    override fun handle(update: Update, user: User?) {
        subscriptionService.remindFillingProfile()
    }

    companion object {
        /** Команда запуска составления пар участников. */
        const val COMMAND = "/rematch"
    }
}
