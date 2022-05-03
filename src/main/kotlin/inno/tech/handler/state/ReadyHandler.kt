package inno.tech.handler.state

import inno.tech.TelegramBotApi
import inno.tech.constant.Command
import inno.tech.constant.Message
import inno.tech.constant.Status
import inno.tech.exception.RandomCoffeeBotException
import inno.tech.handler.Handler
import inno.tech.model.User
import inno.tech.repository.UserRepository
import inno.tech.service.SubscriptionService
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * Обработчик сообщения о готовности участвовать в жеребьёвке.
 *
 * @param telegramBotApi компонент, предоставляющий доступ к Telegram Bot API
 */
@Component
class ReadyHandler(
    private val telegramBotApi: TelegramBotApi,
    private val userRepository: UserRepository,
    private val subscriptionService: SubscriptionService,
) : Handler {

    override fun accept(command: String, user: User?): Boolean {
        return user != null && user.status == Status.ASKED && command == Command.READY.command
    }

    override fun handle(update: Update, user: User?) {
        if (user == null) {
            throw RandomCoffeeBotException("user cannot be null")
        }

        val readyUser = userRepository.findAllByStatusAndActiveTrue(Status.READY).firstOrNull()
        if (fastMatchingAvailable() && readyUser != null) {
            //fast matching
            subscriptionService.sendInvitation(readyUser, user)
        } else {
            //just change status
            user.status = Status.READY

            val pauseReply = SendMessage()
            pauseReply.text = Message.READY_TO_MATCH
            pauseReply.parseMode = ParseMode.MARKDOWN
            pauseReply.chatId = user.chatId.toString()
            telegramBotApi.execute(pauseReply)
        }
    }

    private fun fastMatchingAvailable(): Boolean {
        val now = LocalDateTime.now()
        val dayOfWeek = now.dayOfWeek
        val hour = now.hour
        return DayOfWeek.FRIDAY <= dayOfWeek && 7 <= hour || DayOfWeek.SUNDAY >= dayOfWeek && 7 >= hour
    }
}
