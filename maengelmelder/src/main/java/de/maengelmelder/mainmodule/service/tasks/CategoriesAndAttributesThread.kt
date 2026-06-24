package de.maengelmelder.mainmodule.service.tasks

import android.content.Context
import android.util.Log
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.database.MMDBUtils
import de.maengelmelder.mainmodule.objects.Domain

/**
 * Thread used to put Category and Attribute entries to database while also filling the relational table for them
 */
internal class CategoriesAndAttributesThread(c: Context, doms: List<Domain>, done: (() -> Unit)? = null) : Thread() {
    private val domains = doms
    private val db = MMDB.instance(c)
    private val doneFunc = done

    private fun debug(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d("CategoriesAndAttributesThread", msg)
        }
    }

    override fun run() {
        db.writableDatabase.use {
            // Save the domain
            domains.forEach { domain ->
                debug("Removing existing records from domain ${domain.id}...")
                // Remove existing ones first
                db.removeCategoriesAndAttributesFromDomain(domain.systemId, domain.id!!, it)

                // If default domain is to be used as much as possible, we put it on any domain that has no domainid
                if (MMConstants.UseDefaultDomainWhenPossible && (domain.id == null || domain.id?.isEmpty() == true)) {
                    domain.id = MMConstants.DefaultDomainId.toString()
                }
                debug("Adding domain to DB: ${domain.id}...")
                db.addDomain(domain, it)
                domain.categoriesAsArray().forEach { cat ->
                    // Save the category belonging to that domain
                    if (MMConstants.UseDefaultDomainWhenPossible && cat.domainId.isEmpty()) {
                        cat.domainId = MMConstants.DefaultDomainId.toString()
                    }
                    val catId = cat.generateId()
                    debug("- Adding category to DB: $catId...")
                    db.addCategory(cat, it)
                    // Save the attributes belonging to that category,
                    // as well as relation
                    cat.iterateAttributes { attr ->
                        if (MMConstants.UseDefaultDomainWhenPossible && attr.domainId.isEmpty()) {
                            attr.domainId = MMConstants.DefaultDomainId.toString()
                        }
                        val id = attr.generateId()
                        debug("--- Adding Attribute to DB: $id - ${attr.code}...")
                        db.addAtribute(attr, it)
                        db.addAttrCatRelation(id, catId, it)
                    }
                }
            }
        }

        doneFunc?.invoke()
    }
}