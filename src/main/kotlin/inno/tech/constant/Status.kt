package inno.tech.constant

/**
 * Статус пользователя.
 */
enum class Status {

    /** Регистрация. Пользователь ввёл имя */
    REG_NAME,

    /** Регистрация. Пользователь ввёл уровень владения языком */
    REG_LEVEL,

    /** Регистрация. Пользователь ввёл город */
    REG_CITY,

    /** Регистрация. Пользователь вводит дополнительную информацию о себе */
    REG_PROFILE,

    /** Отправлен запрос об участии на следующей неделе */
    ASKED,

    /** Отправлено предложение сменить партнёра */
    SUGGEST_REMATCH,

    /** Пользователь готовый к участию. Пользователь прошедший регистрацию */
    READY,

    /** Пользователь пропускает встречу на этой неделе*/
    SKIP,

    /** Для пользователя запланирована встреча. Пользователю отправлен партнёр для встречи */
    MATCHED,

    /** Пользователь не нашедший пару при жеребьёвке */
    UNPAIRED,
}

/**
 * Основные статусы.
 */
val COMMON_STATUSES = listOf(
    Status.ASKED,
    Status.SUGGEST_REMATCH,
    Status.READY,
    Status.MATCHED,
)
