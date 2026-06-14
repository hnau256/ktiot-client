package org.hnau.ktiot.client.projector.utils

data class Localization(
    val app_name: String = "KtIoT",
    val credentials: String = "Авторизация",
    val login: String = "Вход",
    val address: String = "Адрес",
    val port: String = "Порт",
    val client_id: String = "Идентификатор клиента",
    val user: String = "Пользователь",
    val password: String = "Пароль",
    val logout: String = "Выход",
    val before_reconnection: String = "До переподключения",
    val reconnect_now: String = "Переподключиться сейчас",
    val connecting: String = "Подключение",
    val connection_error: String = "Ошибка подключения",
    val seconds_short: String = "с",
    val minutes_short: String = "м",
    val hours_short: String = "ч",
    val days_short: String = "д",
    val yes: String = "Да",
    val no: String = "Нет",
)