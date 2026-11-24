package com.example.model

data class WorldCountryLabel(
    val code: String,
    val nameAr: String,
    val latitude: Double,
    val longitude: Double,
    val priority: Int // 1 = global smallest zoom, 2 = continental zoom, 3 = regional zoom
)

object WorldCountryCatalog {
    val countries: List<WorldCountryLabel> = listOf(
        // Arab World (22 countries)
        WorldCountryLabel("SA", "السعودية", 24.0, 45.0, 1),
        WorldCountryLabel("IQ", "العراق", 33.2, 43.7, 1),
        WorldCountryLabel("EG", "مصر", 26.8, 30.8, 1),
        WorldCountryLabel("DZ", "الجزائر", 28.0, 2.6, 1),
        WorldCountryLabel("MA", "المغرب", 31.8, -7.1, 1),
        WorldCountryLabel("SD", "السودان", 15.5, 30.2, 1),
        WorldCountryLabel("LY", "ليبيا", 27.0, 17.2, 1),
        WorldCountryLabel("YE", "اليمن", 15.6, 48.0, 2),
        WorldCountryLabel("OM", "عُمان", 21.5, 56.2, 2),
        WorldCountryLabel("MR", "موريتانيا", 20.3, -10.9, 2),
        WorldCountryLabel("SO", "الصومال", 5.2, 46.2, 2),
        WorldCountryLabel("SY", "سوريا", 35.0, 38.5, 2),
        WorldCountryLabel("JO", "الأردن", 31.2, 36.8, 2),
        WorldCountryLabel("TN", "تونس", 34.0, 9.5, 2),
        WorldCountryLabel("AE", "الإمارات", 24.2, 54.4, 2),
        WorldCountryLabel("KW", "الكويت", 29.3, 47.5, 3),
        WorldCountryLabel("QA", "قطر", 25.3, 51.2, 3),
        WorldCountryLabel("BH", "البحرين", 26.0, 50.55, 3),
        WorldCountryLabel("LB", "لبنان", 33.9, 35.85, 3),
        WorldCountryLabel("PS", "فلسطين", 31.9, 35.2, 3),
        WorldCountryLabel("DJ", "جيبوتي", 11.8, 42.6, 3),
        WorldCountryLabel("KM", "جزر القمر", -11.7, 43.3, 3),

        // Europe & Eurasia
        WorldCountryLabel("RU", "روسيا", 61.5, 90.0, 1),
        WorldCountryLabel("TR", "تركيا", 39.0, 35.2, 1),
        WorldCountryLabel("FR", "فرنسا", 46.6, 2.3, 1),
        WorldCountryLabel("DE", "ألمانيا", 51.1, 10.4, 1),
        WorldCountryLabel("ES", "إسبانيا", 40.4, -3.7, 1),
        WorldCountryLabel("IT", "إيطاليا", 42.8, 12.6, 1),
        WorldCountryLabel("GB", "بريطانيا", 54.0, -2.5, 1),
        WorldCountryLabel("UA", "أوكرانيا", 49.0, 31.4, 2),
        WorldCountryLabel("PL", "بولندا", 52.0, 19.4, 2),
        WorldCountryLabel("SE", "السويد", 62.0, 15.0, 2),
        WorldCountryLabel("NO", "النرويج", 64.5, 11.0, 2),
        WorldCountryLabel("FI", "فنلندا", 64.0, 26.0, 2),
        WorldCountryLabel("GR", "اليونان", 39.0, 22.0, 2),
        WorldCountryLabel("PT", "البرتغال", 39.6, -8.0, 2),
        WorldCountryLabel("RO", "رومانيا", 45.9, 24.9, 2),
        WorldCountryLabel("NL", "هولندا", 52.2, 5.3, 3),
        WorldCountryLabel("CH", "سويسرا", 46.8, 8.2, 3),
        WorldCountryLabel("AT", "النمسا", 47.5, 14.5, 3),

        // Asia
        WorldCountryLabel("CN", "الصين", 35.8, 104.2, 1),
        WorldCountryLabel("IN", "الهند", 22.5, 79.0, 1),
        WorldCountryLabel("IR", "إيران", 32.4, 53.7, 1),
        WorldCountryLabel("KZ", "كازاخستان", 48.0, 67.0, 1),
        WorldCountryLabel("JP", "اليابان", 36.2, 138.2, 1),
        WorldCountryLabel("ID", "إندونيسيا", -2.5, 118.0, 1),
        WorldCountryLabel("PK", "باكستان", 30.4, 69.3, 2),
        WorldCountryLabel("AF", "أفغانستان", 33.9, 67.7, 2),
        WorldCountryLabel("TH", "تايلاند", 15.8, 101.0, 2),
        WorldCountryLabel("VN", "فيتنام", 16.0, 107.8, 2),
        WorldCountryLabel("MY", "ماليزيا", 4.2, 102.0, 2),
        WorldCountryLabel("KR", "كوريا الجنوبية", 36.5, 127.8, 2),
        WorldCountryLabel("PH", "الفلبين", 12.8, 121.8, 2),
        WorldCountryLabel("MN", "منغوليا", 46.8, 103.8, 2),
        WorldCountryLabel("UZ", "أوزبكستان", 41.4, 64.6, 2),
        WorldCountryLabel("SG", "سنغافورة", 1.35, 103.82, 3),

        // Africa
        WorldCountryLabel("NG", "نيجيريا", 9.1, 8.7, 1),
        WorldCountryLabel("ET", "إثيوبيا", 9.1, 40.5, 1),
        WorldCountryLabel("ZA", "جنوب أفريقيا", -29.0, 24.0, 1),
        WorldCountryLabel("KE", "كينيا", 0.2, 37.9, 1),
        WorldCountryLabel("CD", "الكونغو", -3.0, 23.5, 1),
        WorldCountryLabel("TD", "تشاد", 15.4, 18.7, 2),
        WorldCountryLabel("ML", "مالي", 17.6, -4.0, 2),
        WorldCountryLabel("NE", "النيجر", 17.6, 8.1, 2),
        WorldCountryLabel("TZ", "تنزانيا", -6.4, 34.9, 2),
        WorldCountryLabel("AO", "أنغولا", -12.3, 17.5, 2),
        WorldCountryLabel("NA", "ناميبيا", -22.6, 17.1, 2),
        WorldCountryLabel("MG", "مدغشقر", -19.0, 46.7, 2),
        WorldCountryLabel("SN", "السنغال", 14.5, -14.4, 2),

        // Americas & Oceania
        WorldCountryLabel("CA", "كندا", 56.1, -106.3, 1),
        WorldCountryLabel("US", "أمريكا", 39.8, -98.6, 1),
        WorldCountryLabel("MX", "المكسيك", 23.6, -102.5, 1),
        WorldCountryLabel("BR", "البرازيل", -14.2, -51.9, 1),
        WorldCountryLabel("AR", "الأرجنتين", -35.0, -65.0, 1),
        WorldCountryLabel("CO", "كولومبيا", 4.6, -74.3, 2),
        WorldCountryLabel("PE", "بيرو", -9.2, -75.0, 2),
        WorldCountryLabel("CL", "تشيلي", -33.5, -70.6, 2),
        WorldCountryLabel("VE", "فنزويلا", 6.4, -66.6, 2),
        WorldCountryLabel("AU", "أستراليا", -25.3, 133.8, 1),
        WorldCountryLabel("NZ", "نيوزيلندا", -41.5, 172.5, 2)
    )
}
