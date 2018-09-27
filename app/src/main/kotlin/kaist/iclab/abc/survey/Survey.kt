package kaist.iclab.abc.survey

import android.text.TextUtils
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import kaist.iclab.abc.common.EmptySurveyException
import kaist.iclab.abc.common.InvalidSurveyFormatException
import java.util.*

data class Survey (
        val title: String,
        val message: String? = "",
        val instruction: String? = "",
        val policy: SurveyPolicy = SurveyPolicy(),
        val questions: ArrayList<SurveyQuestion> = arrayListOf()
) {
    companion object {
        fun parse(surveyString: String?) : Survey {
            return try {
                if(TextUtils.isEmpty(surveyString)) throw EmptySurveyException()
                val survey = Klaxon().parse<Survey>(surveyString!!) ?: throw InvalidSurveyFormatException()
                if(TextUtils.isEmpty(survey.title)) throw InvalidSurveyFormatException()
                survey
            } catch (e: KlaxonException) { throw InvalidSurveyFormatException() }
        }
    }

    fun toJson() : String {
        return try {
            Klaxon().toJsonString(this)
        } catch (e: Exception) {
            ""
        }
    }
}
