package com.transcend.otg.utilities

class FileNameChecker(private val mInput: String) {

    val isContainInvalid: Boolean
        get() {
            for (i in INVALID_CHARS.indices) {
                if (mInput.contains(INVALID_CHARS[i].toString())) {
                    return true
                }
            }
            return false
        }

    val isStartWithSpace: Boolean
        get() = mInput.startsWith(" ")

    companion object {
        private val INVALID_CHARS = arrayOf(
            '!',
            '*',
            '\'',
            ';',
            ':',
            '@',
            '&',
            '=',
            '+',
            '$',
            ',',
            '/',
            '?',
            '#',
            '<',
            '>',
            '%',
            '|',
            '\\',
            '^',
            '`',
            '！',
            '＊',
            '；',
            '：',
            '＠',
            '＆',
            '＝',
            '＋',
            '＄',
            '，',
            '／',
            '？',
            '＃',
            '＜',
            '＞',
            '％',
            '｜',
            '︿',
            '‘'
        )
    }
}
