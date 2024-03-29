package inno.tech.handler.registration

import inno.tech.constant.Level
import inno.tech.constant.Status
import inno.tech.constant.message.MessageProvider
import inno.tech.exception.RandomCoffeeBotException
import inno.tech.extension.createBtn
import inno.tech.extension.getChatIdAsString
import inno.tech.extension.getMessageText
import inno.tech.handler.Handler
import inno.tech.handler.registration.InputCityHandler.Companion.CITIES
import inno.tech.model.User
import inno.tech.service.message.MessageService
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup

/**
 * Обработчик сообщений заполнения уровня владения языком.
 *
 * @param messageService сервис отправки сообщений
 * @param messageProvider компонент, содержащий шаблоны сообщений
 */
@Component
class InputLevelHandler(
    private val messageService: MessageService,
    private val messageProvider: MessageProvider,
) : Handler {

    override fun accept(command: String, user: User?): Boolean {
        return user != null && Status.REG_LEVEL == user.status
    }

    override fun handle(update: Update, user: User?) {
        if (user == null) {
            throw RandomCoffeeBotException("Error user state for message $update")
        }

        val level = try {
            Level.valueOf(update.getMessageText())
        } catch (e: IllegalArgumentException) {
            messageService.sendMessageWithKeyboard(update.getChatIdAsString(), LEVELS, messageProvider.regStepLevel)
            return
        }

        user.level = level
        user.status = Status.REG_CITY

        messageService.sendMessageWithKeyboard(update.getChatIdAsString(), CITIES, messageProvider.regStepCity)
    }

    companion object {

        val LEVELS = chooseLevelBtn()

        private fun chooseLevelBtn(): InlineKeyboardMarkup {
            return InlineKeyboardMarkup().apply {
                keyboard = listOf(
                    listOf(createBtn(Level.A1.toString()), createBtn(Level.A2.toString())),
                    listOf(createBtn(Level.B1.toString()), createBtn(Level.B2.toString())),
                    listOf(createBtn(Level.C1.toString()), createBtn(Level.C2.toString())),
                )
            }
        }
    }
}
