package com.mwnl.rttmas_android.core

import java.util.Locale

class LicensePlatePostprocessor {

    companion object {
        /**
         * Entrypoint for license plate text postprocessing.
         *
         * @param [String] rawText - The input license plate text
         * @param [String] region - The region which the license plate belongs to
         *
         * Available regions:
         * - tw (Taiwan, the default option)
         * - hk (Hong Kong)
         *
         * @return [String] The postprocessed license plate text
         */
        fun postprocessLicensePlateText(rawText: String, region: String = "tw"): String {

            // First, remove blanks, line-breaks and dashes
            val lpText = rawText.uppercase(Locale.ROOT)
                .replace("\n", "")
                .replace(" ", "")
                .replace("-", "")

            // Initialize the result variable
            var result = ""

            // Apply postprocessing rules according to the region
            result = when (region) {
                "tw" -> postprocessTaiwanLicensePlate(lpText)
                "hk" -> postprocessHongKongLicensePlate(lpText)

                else -> postprocessTaiwanLicensePlate(lpText)
            }

            // Replace symbols that are similar to alphabets
            result = result.replace("|", "1").replace("&", "8")

            // Remove all other symbols
            result = result.replace(Regex("\\p{Punct}"), "")

            // Normalize alphabet variants to normal ones (e.g. 'Å' to 'A')
            result = result.map { normalizationMap[it] ?: it }.joinToString("")

            return result
        }


        /**
         * Taiwan license plate rules.
         *
         * Rules:
         * - Length: 4 to 7 characters
         *
         * @param [String] lpText - The input license plate text
         * @return [String] The postprocessed license plate text
         */
        private fun postprocessTaiwanLicensePlate(lpText: String): String {
            if (lpText.length < 4 || lpText.length > 7)
                return ""

            var result = ""

            if (lpText.length == 7) {
                result += lpText.substring(0, 3).replace("0", "Q").replace("5", "S")
                    .replace("8", "B").replace("1", "I").replace("2", "Z")
                result += lpText.substring(3, 7).replace("O", "0").replace("S", "5")
                    .replace("B", "8").replace("I", "1").replace("Z", "2").replace("J", "1")
            } else result = lpText

            return result
        }


        /**
         * Hong Kong license plate rules.
         *
         * Rules:
         * - Length: 1 to 8 characters
         * - Exclude alphabets: 'O', 'Q', 'I'
         *
         * @param [String] lpText - The input license plate text
         * @return [String] The postprocessed license plate text
         */
        private fun postprocessHongKongLicensePlate(lpText: String): String {
            if (lpText.length < 4 || lpText.length > 7)
                return ""

            var result = lpText
            result = result.replace("I", "1").replace("O", "0").replace("Q", "0")

            return result
        }


        // A map of alphabet variants
        private val normalizationMap = mapOf(
            'Å' to 'A', 'å' to 'a',
            'Ä' to 'A', 'ä' to 'a',
            'Ö' to 'O', 'ö' to 'o',
            'Ü' to 'U', 'ü' to 'u',
            'Ñ' to 'N', 'ñ' to 'n',
            'Ç' to 'C', 'ç' to 'c',
            'É' to 'E', 'é' to 'e',
            'Í' to 'I', 'í' to 'i',
            'Ó' to 'O', 'ó' to 'o',
            'Ú' to 'U', 'ú' to 'u',
            'À' to 'A', 'à' to 'a',
            'Â' to 'A', 'â' to 'a',
            'Æ' to "AE", 'æ' to "ae",
            'Ê' to 'E', 'ê' to 'e',
            'Ì' to 'I', 'ì' to 'i',
            'Ò' to 'O', 'ò' to 'o',
            'Ô' to 'O', 'ô' to 'o',
            'Õ' to 'O', 'õ' to 'o',
            'Ù' to 'U', 'ù' to 'u',
            'Û' to 'U', 'û' to 'u',
            'Ý' to 'Y', 'ý' to 'y', 'ÿ' to 'y',
            'Š' to 'S', 'š' to 's',
            'Ž' to 'Z', 'ž' to 'z',
            'Þ' to "Th", 'þ' to "th",
            'Đ' to 'D', 'đ' to 'd',
            'Ł' to 'L', 'ł' to 'l',
            'Ň' to 'N', 'ň' to 'n',
            'Ō' to 'O', 'ō' to 'o',
            'Ŕ' to 'R', 'ŕ' to 'r',
            'Ť' to 'T', 'ť' to 't',
            'Ź' to 'Z', 'ź' to 'z',
        )

    }
}