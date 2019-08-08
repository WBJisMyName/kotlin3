package com.transcend.otg.action.loader

import android.content.Context
import androidx.loader.content.AsyncTaskLoader
import com.transcend.otg.data.FileRepository
import com.transcend.otg.utilities.MainApplication
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.util.*

abstract class LocalAbstractLoader(context: Context) : AsyncTaskLoader<Boolean>(context) {

    val repository = FileRepository(MainApplication())

    protected fun createUniqueName(source: File, destination: String): String? {
        val isDirectory = source.isDirectory
        val dir = File(destination)
        val files = dir.listFiles { pathname -> pathname.isDirectory == isDirectory }
        val names = ArrayList<String>()
        for (file in files) names.add(file.name)
        val origin = source.name
        var unique = origin
        val ext = FilenameUtils.getExtension(origin)
        val prefix = FilenameUtils.getBaseName(origin)
        val suffix = if (ext.isEmpty()) "" else String.format(".%s", ext)
        var index = 1
        while (names.contains(unique)) {
            unique = String.format("$prefix (%d)$suffix", index++)
        }
        return unique
    }
}
