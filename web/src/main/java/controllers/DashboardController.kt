package controllers

import javax.inject.Singleton
import javax.inject.Inject
import com.google.inject.Provider
import javax.persistence.EntityManager
import ninja.Result
import ninja.Results
import ninja.jpa.UnitOfWork
import models.Crash
import com.google.common.collect.Multiset
import com.google.common.collect.HashMultiset
import com.google.common.collect.Multisets
import javafx.scene.paint.Color

Singleton
open class DashboardController [Inject] (val em: Provider<EntityManager>) {
    fun colorToWeb(color: Color): String {
        return "#%x%x%x".format((color.getRed()*255).toInt(), (color.getGreen()*255).toInt(), (color.getBlue()*255).toInt())
    }

    UnitOfWork
    open fun render(): Result {

        val query = em.get().createQuery("SELECT x FROM Crash x ORDER BY x.timestamp DESC", javaClass<Crash>())
        query.setMaxResults(100)
        val crashes = query.getResultList()

        // Calc top exception types
        val ms: Multiset<String> = HashMultiset.create(crashes.map { it.exceptionTypeName }.filterNotNull())
        val tops = Multisets.copyHighestCountFirst(ms).entrySet().map { Pair(it.getCount(), it.getElement()) }

        [data] class TopCrash(val count: Int, val type: String, val color: String, val colorHighlight: String)
        val renderObjs: MutableList<TopCrash> = linkedListOf()
        var color = Color.CORNFLOWERBLUE;
        for (top in tops) {
            val lightColor = color.brighter();
            renderObjs.add(TopCrash(top.first, top.second.trim(), colorToWeb(color), colorToWeb(lightColor)))
            color = color.desaturate();
        }
        return Results.html().template("views/dashboard.html.ftl").render(mapOf("crashes" to crashes, "tops" to renderObjs))
    }
}