package ch.softhenge.solar.i18n

import com.vaadin.flow.i18n.I18NProvider
import org.springframework.stereotype.Component
import java.text.MessageFormat
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

@Component
class TranslationProvider : I18NProvider {

    override fun getProvidedLocales(): List<Locale> = listOf(Locale.ENGLISH)

    override fun getTranslation(key: String, locale: Locale, vararg params: Any): String {
        return try {
            val bundle = ResourceBundle.getBundle("messages", Locale.ENGLISH)
            val value  = bundle.getString(key)
            if (params.isEmpty()) value else MessageFormat.format(value, *params)
        } catch (e: MissingResourceException) {
            "!$key!"
        }
    }
}
