package conf

import com.google.inject.AbstractModule
import com.google.inject.Singleton
import ninja.UsernamePasswordValidator
import com.google.inject.Inject
import ninja.utils.NinjaProperties
import kotlin.properties.Delegates

Singleton
public class Module : AbstractModule() {
    override fun configure() {
        bind(javaClass<UsernamePasswordValidator>()).toInstance(object : UsernamePasswordValidator {
            Inject var properties: NinjaProperties? = null

            override fun validateCredentials(username: String, password: String): Boolean {
                val pass = properties!!.get("dashboardPassword")
                if (pass == "CHANGE_ME") throw Exception("Change the default password")
                return (username == "admin" && password == pass )
            }
        })
    }
}
