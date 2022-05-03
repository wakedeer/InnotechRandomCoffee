package inno.tech

import inno.tech.constant.Command
import inno.tech.constant.Message
import inno.tech.properties.TelegramProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

/**
 * Компонент, предоставляющий доступ к Telegram Bot API
 *
 * @param telegramProperties конфигурации подключения к Telegram Bot API
 */
@Component
class TelegramBotApi(
    private val telegramProperties: TelegramProperties,
) : TelegramLongPollingBot() {

    /** Обработчик входящий сообщений. (Lazy для избежания рекурсивной зависимости) */
    @Lazy
    @Autowired
    private lateinit var messageHandler: MessageHandler

    override fun getBotToken() = telegramProperties.token

    override fun getBotUsername() = telegramProperties.name

    override fun onUpdateReceived(update: Update) {
        messageHandler.handle(update)
    }

    /**
     * Отправляет главное меню бота, если обработчик сообщения не найден.
     */
    fun errorSend(chatId: Long) {
        val response = SendMessage()
        response.text = Message.ERROR
        response.replyMarkup = MAIN_MENU
        response.parseMode = ParseMode.MARKDOWN
        response.chatId = chatId.toString()
        execute(response)
    }

    companion object {

        val MAIN_MENU: InlineKeyboardMarkup = mainMenu()

        /**
         * Возвращает главное меню бота.
         * @return главное меню бота
         */
        private fun mainMenu(): InlineKeyboardMarkup {
            val infoBtn = InlineKeyboardButton()
            infoBtn.text = "Что такое Random coffee"
            infoBtn.callbackData = Command.INFO.command

            val showProfileBtn = InlineKeyboardButton()
            showProfileBtn.text = "Посмотреть свой профиль"
            showProfileBtn.callbackData = Command.SHOW_PROFILE.command

            val editProfileBtn = InlineKeyboardButton()
            editProfileBtn.text = "Поменять данные профиля"
            editProfileBtn.callbackData = Command.EDIT_PROFILE.command

            val pauseBtn = InlineKeyboardButton()
            pauseBtn.text = "Поставить бот на паузу"
            pauseBtn.callbackData = Command.PAUSE.command

            val resumeBtn = InlineKeyboardButton().apply {
                text = "Снять бота с паузы"
                callbackData = Command.RESUME.command
            }

            return InlineKeyboardMarkup().apply {
                keyboard = listOf(
                    listOf(infoBtn),
                    listOf(showProfileBtn),
                    listOf(editProfileBtn),
                    listOf(pauseBtn),
                    listOf(resumeBtn),
                )
            }
        }
    }
}
