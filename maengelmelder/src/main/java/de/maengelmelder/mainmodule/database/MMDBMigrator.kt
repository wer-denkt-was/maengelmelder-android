package de.maengelmelder.mainmodule.database

import android.database.sqlite.SQLiteDatabase

/**
 * Migrator which holds a list of patches matched by version number.
 */
internal object MMDBMigrator {

    /**
     * @property mPatchList List of [Patch]es matched by version number.
     * E.g. mPatchList[5] contains patch for database version migration from 5 to 6
     */
    private val mPatchList: HashMap<Int, Patch> = hashMapOf()

    init {
        // Previous version before this class is made. It's better to just rebuild the whole tables
        (0..10).forEach { idx ->
            mPatchList[idx] = object : Patch {
                override fun apply(db: SQLiteDatabase?) { }
                override fun shouldRebuildTables(): Boolean = true
            }
        }

        // Version change 11 -> 12
        mPatchList[11] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_categories ADD COLUMN grp_name TEXT DEFAULT ''")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }

        // Version change 12 -> 13
        mPatchList[12] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        MMDBUtils.dropTable(this, "tbl_systems", true)
                        MMDBUtils.createTable(this, "tbl_domains", true,
                                "_id" to TEXT + PRIMARY_KEY,
                                "name" to TEXT,
                                "domain_url" to TEXT,
                                "private" to INTEGER + DEFAULT("1"))
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }

        // Version change 13 -> 14
        mPatchList[13] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_domains RENAME TO tbl_systems")
                        MMDBUtils.createTable(this,"tbl_domains", true,
                                "_id" to TEXT + PRIMARY_KEY,
                                "system_id" to TEXT,
                                "name" to TEXT,
                                "domain_url" to TEXT,
                                "is_default" to INTEGER + DEFAULT("0"),
                                "is_default_recipient" to INTEGER + DEFAULT("0"),
                                "extras" to TEXT)
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }

        // Version change 14->15
        mPatchList[14] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_systems ADD COLUMN appid INTEGER DEFAULT 1")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }

        // Version change 15->16
        mPatchList[15] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_attributes ADD COLUMN valueslist TEXT DEFAULT ''")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }

        // Version change 16->17
        mPatchList[16] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_messages ADD COLUMN message_internal_type TEXT DEFAULT 'defect_report'")
                        execSQL("UPDATE tbl_messages SET message_internal_type = 'defect_report' WHERE message_internal_type = ''")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }

        // Version change 17->18
        mPatchList[17] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_categories ADD COLUMN external_url TEXT DEFAULT ''")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }

        // Version change 18->19
        mPatchList[18] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_categories ADD COLUMN message_creation_attr_ids TEXT DEFAULT ''")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }

        // Version change 19->20
        mPatchList[19] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_messages ADD COLUMN bms_settings TEXT DEFAULT ''")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }

        // Version change 20->21
        mPatchList[20] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_attributes ADD COLUMN ordering INTEGER DEFAULT 0")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }
        // Version change 21->22
        mPatchList[21] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        MMDBUtils.createTable(this, "tbl_geometries", true,
                                "system_id" to TEXT,
                                "domain_id" to TEXT,
                                "geomid" to INTEGER,
                                "geompartid" to TEXT,
                                "wkt" to TEXT,
                                "min_lat" to REAL,
                                "max_lat" to REAL,
                                "min_lon" to REAL,
                                "max_lon" to REAL,
                                "cat_id" to TEXT // Comma-separated category IDs
                        )
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }
        // Version change 21->23
        mPatchList[22] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_categories ADD COLUMN rubric TEXT DEFAULT ''")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }
        mPatchList[23] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_attributes ADD COLUMN multiselect INTEGER DEFAULT 0")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }
        // 24 -> 25
        mPatchList[24] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_categories ADD COLUMN require_position TEXT DEFAULT 'required'")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }
        // 25 -> 26
        mPatchList[25] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_messages ADD COLUMN marker_url TEXT DEFAULT ''")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }
        // 26 -> 27
        mPatchList[26] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_attributes ADD COLUMN max_length TEXT DEFAULT 0")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }
        // 27 -> 28
        mPatchList[27] = object : Patch {
            override fun apply(db: SQLiteDatabase?) {
                MMDBUtils.transaction(db) {
                    try {
                        execSQL("ALTER TABLE tbl_categories ADD COLUMN create_directlink_only INTEGER DEFAULT 0")
                    } catch (e: Exception) { }
                }
            }
            override fun shouldRebuildTables(): Boolean = false
        }
        // Add more version changes here
    }

    /**
     * Return the [Patch] for a given version or null if it doesn't have any
     */
    fun getPatch(version: Int): Patch? = mPatchList[version]

    /**
     * Interface for the migration patches
     */
    interface Patch {
        /**
         * This method should apply SQLite operations to the given [SQLiteDatabase] parameter in order to apply changes to the database
         */
        fun apply(db: SQLiteDatabase?)

        /**
         * This method should return true if the tables should be rebuilt rather than applying patch. During implementation, you can ignore
         * [Patch.apply] function if this returns true
         */
        fun shouldRebuildTables(): Boolean
    }


}