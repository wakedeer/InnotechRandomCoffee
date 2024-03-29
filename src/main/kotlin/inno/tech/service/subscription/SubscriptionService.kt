package inno.tech.service.subscription

import inno.tech.constant.Command
import inno.tech.constant.Level
import inno.tech.constant.SendInvitationStatus
import inno.tech.constant.Status
import inno.tech.constant.message.MessageProvider
import inno.tech.model.Meeting
import inno.tech.model.User
import inno.tech.repository.MeetingRepository
import inno.tech.repository.TopicRepository
import inno.tech.repository.UserRepository
import inno.tech.service.message.MessageService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import java.time.LocalDateTime
import java.util.LinkedList


/**
 * Сервис для рассылки уведомлений пользователем по расписанию.
 * @param userRepository репозиторий для работы с информацией о пользователе
 * @param meetingRepository репозиторий для работы с информацией о встречах
 * @param messageService сервис отправки сообщений
 * @param topicRepository репозиторий для работы с топиками вопросов
 * @param messageProvider компонент, содержащий шаблоны сообщений
 */
@Service
class SubscriptionService(
    private val userRepository: UserRepository,
    private val meetingRepository: MeetingRepository,
    private val messageService: MessageService,
    private val topicRepository: TopicRepository,
    private val messageProvider: MessageProvider,
) {

    /** Logger. */
    private val log = KotlinLogging.logger {}

    @Transactional
    @Scheduled(cron = "\${schedule.match}")
    fun matchPairs() {
        log.info("Pair matching is started")

        val nextLevelParticipants = mutableListOf<User>()
        for (level in Level.values()) {
            val participants = LinkedList(nextLevelParticipants)
            participants.addAll(userRepository.findAllByStatusAndLevelAndActiveTrue(Status.READY, level))
            nextLevelParticipants.clear()

            while (participants.count() > 1) {

                val firstUser = participants.removeFirst()
                val secondUser = participants.find { candidate -> meetingRepository.existsMeeting(firstUser.userId, candidate.userId).not() }

                if (secondUser == null) {
                    log.debug("Can't find pair for user ${firstUser.userId}")
                    nextLevelParticipants.add(firstUser)
                    continue
                }

                when (sendInvitation(firstUser, secondUser)) {
                    SendInvitationStatus.OK -> {
                        participants.remove(secondUser)
                        log.info("Invitation has been sent to ${firstUser.userId} and ${secondUser.userId} ")
                    }

                    SendInvitationStatus.FIRST_ERROR -> {
                        firstUser.active = false
                        log.warn("User ${firstUser.userId} has been deactivated ")
                    }

                    SendInvitationStatus.SECOND_ERROR -> {
                        secondUser.active = false
                        participants.addFirst(firstUser)
                        participants.remove(secondUser)
                        log.warn("User ${secondUser.userId} has been deactivated ")
                    }
                }
            }
            nextLevelParticipants.addAll(participants)
        }

        // set unscheduled status for other
        nextLevelParticipants.forEach { participant: User ->
            log.info("User ${participant.userId} hasn't been matched")
        }

        log.info("Pair matching is finished successfully")
    }

    fun sendInvitation(firstUser: User, secondUser: User): SendInvitationStatus {
        val topic = topicRepository.getRandomTopic()
        try {
            messageService.sendInvitationMessage(firstUser, secondUser, topic)
        } catch (e: Exception) {
            log.error("Error occurred sending match message to pair ${firstUser.userId} and ${secondUser.userId}", e)
            return SendInvitationStatus.FIRST_ERROR
        }

        try {
            messageService.sendInvitationMessage(secondUser, firstUser, topic)
        } catch (e: Exception) {
            messageService.sendMessage(firstUser.userId.toString(), messageProvider.matchFailureSendToPartner)
            log.error("Error occurred sending match message to pair ${firstUser.userId} and ${secondUser.userId}", e)
            return SendInvitationStatus.SECOND_ERROR
        }

        firstUser.status = Status.MATCHED
        secondUser.status = Status.MATCHED
        meetingRepository.save(Meeting(userId1 = firstUser.userId, userId2 = secondUser.userId, topic = topic))

        log.info("Created pair first user id: ${firstUser.userId} and second user id: ${secondUser.userId}")

        return SendInvitationStatus.OK
    }

    @Transactional
    @Scheduled(cron = "\${schedule.invite}")
    fun sendInvitation() {
        log.info("Invention sending is started")
        val invitationGroup = listOf(Status.MATCHED, Status.ASKED, Status.SUGGEST_REMATCH, Status.SKIP)
        val participants = userRepository.findAllByStatusInAndActiveTrue(invitationGroup)
        participants.forEach { participant: User ->
            participant.status = Status.ASKED
            try {
                messageService.sendMessageWithKeyboard(participant.chatId.toString(), SUGGESTION_MENU, messageProvider.matchSuggestion)
            } catch (ex: Exception) {
                handleError(ex, participant)
            }
        }
        log.info("Invention sending has finished")
    }

    @Transactional
    @Scheduled(cron = "\${schedule.rematch}")
    fun rematch() {
        log.info("Rematching is started")
        val invitationGroup = listOf(Status.MATCHED)
        val participants = userRepository.findAllByStatusInAndActiveTrue(invitationGroup)
        participants.forEach { participant: User ->
            participant.status = Status.SUGGEST_REMATCH
            try {
                messageService.sendMessageWithKeyboard(participant.chatId.toString(), REMATCH_MENU, messageProvider.rematchSuggestion)
            } catch (ex: Exception) {
                handleError(ex, participant)
            }
        }
        log.info("Rematching has finished")
    }

    @Transactional
    @Scheduled(cron = "\${schedule.remind}")
    fun remindFillingProfile() {
        log.info("Reminding is started")
        val registrationStatusGroup = listOf(Status.REG_NAME, Status.REG_LEVEL, Status.REG_CITY, Status.REG_PROFILE)
        val uncompletedProfileUsers = userRepository.findAllByStatusInAndActiveTrue(registrationStatusGroup)
        val thresholdTime = LocalDateTime.now().minusHours(1)
        uncompletedProfileUsers.asSequence()
            .filter { user -> thresholdTime.isAfter(user.regDate) }
            .forEach { user: User ->
                try {
                    messageService.sendMessageWithKeyboard(user.chatId.toString(), REMIND_FILL_PROFILE_MENU, messageProvider.remindFillingProfile)
                } catch (ex: Exception) {
                    handleError(ex, user)
                }
            }

        log.info("Reminding has finished")
    }

    /**
     * Метод обработчик ошибок отправки сообщения.
     * В случае ошибки от Telegram деактивируем пользователя.
     *
     * @param ex исключение
     * @param participant получатель сообщения
     */
    private fun handleError(ex: Exception, participant: User) {
        if (ex is TelegramApiRequestException && 403 == ex.errorCode) {
            log.warn("User ${participant.userId} unsubscribed from the bot. Deactivate user", ex)
            participant.active = false
            participant.status = Status.DEACTIVATED
        } else {
            log.error("Sending a request. Error occurred with user ${participant.userId} ", ex)
        }
    }

    companion object {

        /** Сообщение о готовности участвовать в жеребьевке. */
        private const val READY_MESSAGE = "Yes, sure"

        /** Сообщение о пропуске участия в жеребьёвке. */
        private const val SKIP_MESSAGE = "Skip one week"

        /** Сообщение, что партнёры договорились. */
        private const val OK_MESSAGE = "Yes, we have agreed"

        /** Сообщение продолжения регистрации. */
        private const val CONTINUE = "Continue"

        /** Сообщение о том, что партнёр не ответил. */
        private const val NO_RESPONSE_MESSAGE = "No response"

        /** Меню выбора участия в жеребьёвке. */
        val SUGGESTION_MENU = run {
            val infoBtn = InlineKeyboardButton().apply {
                text = READY_MESSAGE
                callbackData = Command.READY.command
            }
            val showProfileBtn = InlineKeyboardButton().apply {
                text = SKIP_MESSAGE
                callbackData = Command.SKIP.command
            }
            InlineKeyboardMarkup().apply {
                keyboard = listOf(listOf(infoBtn, showProfileBtn))
            }
        }

        /** Меню выбора смены партнёра. */
        val REMATCH_MENU = run {
            val infoBtn = InlineKeyboardButton().apply {
                text = OK_MESSAGE
                callbackData = Command.SKIP_REMATCH.command
            }
            val showProfileBtn = InlineKeyboardButton().apply {
                text = NO_RESPONSE_MESSAGE
                callbackData = Command.REQUEST_REMATCH.command
            }
            InlineKeyboardMarkup().apply {
                keyboard = listOf(listOf(infoBtn), listOf(showProfileBtn))
            }
        }

        /** Меню напоминания закончить заполнение профиля. */
        val REMIND_FILL_PROFILE_MENU = run {
            val infoBtn = InlineKeyboardButton().apply {
                text = CONTINUE
                callbackData = Command.EDIT_PROFILE.command
            }
            InlineKeyboardMarkup().apply {
                keyboard = listOf(listOf(infoBtn))
            }
        }
    }
}
