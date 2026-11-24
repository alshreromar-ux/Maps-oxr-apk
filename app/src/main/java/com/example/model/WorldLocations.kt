package com.example.model

import androidx.annotation.StringRes
import com.example.R

enum class WorldRegion(@StringRes val labelRes: Int, val emoji: String) {
    ALL(R.string.world_region_all, "🌍"),
    ARAB_WORLD(R.string.world_region_arab, "🌙"),
    EUROPE(R.string.world_region_europe, "🏰"),
    ASIA(R.string.world_region_asia, "🏯"),
    AFRICA(R.string.world_region_africa, "🦁"),
    NORTH_AMERICA(R.string.world_region_north_america, "🗽"),
    SOUTH_AMERICA(R.string.world_region_south_america, "🌴"),
    OCEANIA(R.string.world_region_oceania, "🦘")
}

data class WorldCity(
    val id: String,
    val countryName: String,
    val cityName: String,
    val flagEmoji: String,
    val region: WorldRegion,
    val location: LatLngPoint,
    val subtitle: String = "",
    val sampleCategory: PinCategory = PinCategory.CAFE,
    val sampleStatus: PinStatus = PinStatus.OPEN,
    val sampleTitle: String = "",
    val sampleDescription: String = "",
    val isFromGlobalSearch: Boolean = false
)

fun countryCodeToFlagEmoji(countryCode: String?): String {
    val clean = countryCode?.trim()?.uppercase().orEmpty()
    if (clean.length != 2 || !clean.all { it in 'A'..'Z' }) return "🌍"
    val first = Character.codePointAt(clean, 0) - 'A'.code + 0x1F1E6
    val second = Character.codePointAt(clean, 1) - 'A'.code + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

object WorldCatalog {
    val WORLD_OVERVIEW_CENTER = LatLngPoint(22.0, 18.0)
    const val WORLD_OVERVIEW_ZOOM = 2.0

    val ARAB_WORLD_OVERVIEW_CENTER = LatLngPoint(25.8, 28.5)
    const val ARAB_WORLD_OVERVIEW_ZOOM = 3.7

    private val quickPickIds = setOf(
        "sa_riyadh", "iq_baghdad", "eg_cairo", "ae_dubai",
        "ma_casablanca", "jo_amman", "gb_london", "fr_paris",
        "tr_istanbul", "jp_tokyo", "us_nyc", "au_sydney"
    )

    val quickPickCities: List<WorldCity> by lazy {
        cities.filter { it.id in quickPickIds }
    }

    val cities: List<WorldCity> = listOf(
        // ==================== Arab World (All 22 Arab League Countries) ====================
        WorldCity(
            id = "sa_riyadh",
            countryName = "السعودية",
            cityName = "الرياض",
            flagEmoji = "🇸🇦",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(24.7136, 46.6753),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية النبض المناوبة — الرياض",
            sampleDescription = "مناوبة 24 ساعة في حي العليا مع توفر كافة أدوية الطوارئ."
        ),
        WorldCity(
            id = "sa_jeddah",
            countryName = "السعودية",
            cityName = "جدة",
            flagEmoji = "🇸🇦",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(21.4858, 39.1925),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى الكورنيش البحري — جدة",
            sampleDescription = "جلسات مفتوحة مطلة على البحر وخدمة إنترنت سريعة."
        ),
        WorldCity(
            id = "ae_dubai",
            countryName = "الإمارات",
            cityName = "دبي",
            flagEmoji = "🇦🇪",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(25.2048, 55.2708),
            sampleCategory = PinCategory.DEAL,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "عرض لحظي: سوق المرسى — دبي",
            sampleDescription = "خصومات مسائية 30% في المطاعم والمقاهي المطلة على المارينا."
        ),
        WorldCity(
            id = "ae_abudhabi",
            countryName = "الإمارات",
            cityName = "أبوظبي",
            flagEmoji = "🇦🇪",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(24.4539, 54.3773),
            sampleCategory = PinCategory.MAINTENANCE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مركز الخدمة السريعة — أبوظبي",
            sampleDescription = "فحص تكييف وإطارات فوري بدون موعد مسبق."
        ),
        WorldCity(
            id = "iq_baghdad",
            countryName = "العراق",
            cityName = "بغداد",
            flagEmoji = "🇮🇶",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(33.3152, 44.3661),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطعم ومشويات دجلة — بغداد",
            sampleDescription = "مسكوف ومشويات طازجة في الكرادة مع جلسات عائلية هادئة."
        ),
        WorldCity(
            id = "iq_basra",
            countryName = "العراق",
            cityName = "البصرة",
            flagEmoji = "🇮🇶",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(30.5085, 47.7804),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.CROWDED,
            sampleTitle = "مقهى شط العرب — البصرة",
            sampleDescription = "ازدحام مسائي خفيف مع أجواء رائعة على الكورنيش."
        ),
        WorldCity(
            id = "eg_cairo",
            countryName = "مصر",
            cityName = "القاهرة",
            flagEmoji = "🇪🇬",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(30.0444, 31.2357),
            sampleCategory = PinCategory.ROAD_REPORT,
            sampleStatus = PinStatus.TRAFFIC_ALERT,
            sampleTitle = "تحديث مروري: كوبري 6 أكتوبر — القاهرة",
            sampleDescription = "كثافة مرورية متوسطة باتجاه وسط البلد، الطريق الدائري أسرع حالياً."
        ),
        WorldCity(
            id = "eg_alex",
            countryName = "مصر",
            cityName = "الإسكندرية",
            flagEmoji = "🇪🇬",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(31.2001, 29.9187),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "أسماك بحري — الإسكندرية",
            sampleDescription = "عرض الغداء البحري متاح الآن على طريق الكورنيش."
        ),
        WorldCity(
            id = "jo_amman",
            countryName = "الأردن",
            cityName = "عمّان",
            flagEmoji = "🇯🇴",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(31.9454, 35.9284),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى جبل اللويبدة — عمّان",
            sampleDescription = "قهوة مختصة وجلسات هادئة للدراسة والعمل."
        ),
        WorldCity(
            id = "kw_kuwait",
            countryName = "الكويت",
            cityName = "مدينة الكويت",
            flagEmoji = "🇰🇼",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(29.3759, 47.9774),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية السالمية المناوبة — الكويت",
            sampleDescription = "مفتوحة على مدار الساعة مع توفر مستلزمات الأطفال."
        ),
        WorldCity(
            id = "qa_doha",
            countryName = "قطر",
            cityName = "الدوحة",
            flagEmoji = "🇶🇦",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(25.2854, 51.5310),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطعم سوق واقف التراثي — الدوحة",
            sampleDescription = "أطباق خليجية تقليدية وجلسات خارجية مريحة."
        ),
        WorldCity(
            id = "bh_manama",
            countryName = "البحرين",
            cityName = "المنامة",
            flagEmoji = "🇧🇭",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(26.2285, 50.5860),
            sampleCategory = PinCategory.DEAL,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "عرض القهوة والحلويات — المنامة",
            sampleDescription = "خصم 20% في باب البحرين حتى منتصف الليل."
        ),
        WorldCity(
            id = "om_muscat",
            countryName = "عُمان",
            cityName = "مسقط",
            flagEmoji = "🇴🇲",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(23.5880, 58.3829),
            sampleCategory = PinCategory.MAINTENANCE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "ورشة القرم لصيانة المركبات — مسقط",
            sampleDescription = "صيانة ميكانيكية وكهربائية متكاملة بأسعار واضحة."
        ),
        WorldCity(
            id = "lb_beirut",
            countryName = "لبنان",
            cityName = "بيروت",
            flagEmoji = "🇱🇧",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(33.8938, 35.5018),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى الحمرا الثقافي — بيروت",
            sampleDescription = "طاقة شمسية وإنترنت مستقر للعمل عن بُعد."
        ),
        WorldCity(
            id = "sy_damascus",
            countryName = "سوريا",
            cityName = "دمشق",
            flagEmoji = "🇸🇾",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(33.5138, 36.2765),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية الشعلان المناوبة — دمشق",
            sampleDescription = "مناوبة ليلية متواصلة مع خدمة الاستشارات الدوائية."
        ),
        WorldCity(
            id = "ps_jerusalem",
            countryName = "فلسطين",
            cityName = "القدس",
            flagEmoji = "🇵🇸",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(31.7683, 35.2137),
            sampleCategory = PinCategory.DEAL,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "مخبز وحلويات البلدة — القدس",
            sampleDescription = "كنافة ومخبوزات طازجة الآن بخصم مسائي."
        ),
        WorldCity(
            id = "ps_ramallah",
            countryName = "فلسطين",
            cityName = "رام الله",
            flagEmoji = "🇵🇸",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(31.9038, 35.2034),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى المنارة — رام الله",
            sampleDescription = "أجواء هادئة في وسط المدينة."
        ),
        WorldCity(
            id = "ma_casablanca",
            countryName = "المغرب",
            cityName = "الدار البيضاء",
            flagEmoji = "🇲🇦",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(33.5731, -7.5898),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطبخ الأطلس المغربي — الدار البيضاء",
            sampleDescription = "طاجين وكسكس طازج مع خدمة سريعة."
        ),
        WorldCity(
            id = "ma_rabat",
            countryName = "المغرب",
            cityName = "الرباط",
            flagEmoji = "🇲🇦",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(34.0209, -6.8416),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية أكدال الحراسة — الرباط",
            sampleDescription = "صيدلية حراسة ليلية مفتوحة حالياً."
        ),
        WorldCity(
            id = "dz_algiers",
            countryName = "الجزائر",
            cityName = "الجزائر العاصمة",
            flagEmoji = "🇩🇿",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(36.7538, 3.0588),
            sampleCategory = PinCategory.MAINTENANCE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مركز باب الزوار للصيانة — الجزائر",
            sampleDescription = "تبديل زيوت وفحص فرامل سريع."
        ),
        WorldCity(
            id = "tn_tunis",
            countryName = "تونس",
            cityName = "تونس",
            flagEmoji = "🇹🇳",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(36.8065, 10.1815),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى سيدي بوسعيد — تونس",
            sampleDescription = "شاي بالصنوبر وإطلالة بانورامية رائعة."
        ),
        WorldCity(
            id = "ly_tripoli",
            countryName = "ليبيا",
            cityName = "طرابلس",
            flagEmoji = "🇱🇾",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(32.8872, 13.1913),
            sampleCategory = PinCategory.ROAD_REPORT,
            sampleStatus = PinStatus.TRAFFIC_ALERT,
            sampleTitle = "تنبيه طريق الشط — طرابلس",
            sampleDescription = "أعمال صيانة جزئية قرب الميناء، الحركة تسير ببطء."
        ),
        WorldCity(
            id = "sd_khartoum",
            countryName = "السودان",
            cityName = "الخرطوم",
            flagEmoji = "🇸🇩",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(15.5007, 32.5599),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية النيل المركزية — الخرطوم",
            sampleDescription = "توفير الأدوية الأساسية ومستلزمات الإسعاف."
        ),
        WorldCity(
            id = "ye_sanaa",
            countryName = "اليمن",
            cityName = "صنعاء",
            flagEmoji = "🇾🇪",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(15.3694, 44.1910),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "بيت البن اليمني — صنعاء",
            sampleDescription = "بن يمني أصيل محمص طازجاً طوال اليوم."
        ),
        WorldCity(
            id = "ye_aden",
            countryName = "اليمن",
            cityName = "عدن",
            flagEmoji = "🇾🇪",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(12.7855, 45.0187),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطعم ساحل أبين — عدن",
            sampleDescription = "مأكولات بحرية طازجة وإطلالة ساحلية."
        ),
        WorldCity(
            id = "mr_nouakchott",
            countryName = "موريتانيا",
            cityName = "نواكشوط",
            flagEmoji = "🇲🇷",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(18.0735, -15.9582),
            sampleCategory = PinCategory.MAINTENANCE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مركز تفرغ زينة للخدمات — نواكشوط",
            sampleDescription = "صيانة إطارات وبطاريات وكهرباء سيارات."
        ),
        WorldCity(
            id = "so_mogadishu",
            countryName = "الصومال",
            cityName = "مقديشو",
            flagEmoji = "🇸🇴",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(2.0469, 45.3182),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطعم ليدو بيتش — مقديشو",
            sampleDescription = "وجبات طازجة على شاطئ ليدو."
        ),
        WorldCity(
            id = "dj_djibouti",
            countryName = "جيبوتي",
            cityName = "جيبوتي",
            flagEmoji = "🇩🇯",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(11.5721, 43.1456),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية الميناء المناوبة — جيبوتي",
            sampleDescription = "خدمة دوائية متاحة للمسافرين والسكان."
        ),
        WorldCity(
            id = "km_moroni",
            countryName = "جزر القمر",
            cityName = "موروني",
            flagEmoji = "🇰🇲",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(-11.7172, 43.2473),
            sampleCategory = PinCategory.DEAL,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "سوق موروني للتوابل — جزر القمر",
            sampleDescription = "عروض اليوم على المنتجات المحلية الطازجة."
        ),

        // ==================== Europe ====================
        WorldCity(
            id = "gb_london",
            countryName = "المملكة المتحدة",
            cityName = "لندن",
            flagEmoji = "🇬🇧",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(51.5074, -0.1278),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى سوهو المركزي — لندن",
            sampleDescription = "قهوة مختصة ومخبوزات ساخنة في قلب لندن."
        ),
        WorldCity(
            id = "fr_paris",
            countryName = "فرنسا",
            cityName = "باريس",
            flagEmoji = "🇫🇷",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(48.8566, 2.3522),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مخبز ومطعم الشانزليزيه — باريس",
            sampleDescription = "كرواسان طازج ووجبات فرنسية طوال اليوم."
        ),
        WorldCity(
            id = "de_berlin",
            countryName = "ألمانيا",
            cityName = "برلين",
            flagEmoji = "🇩🇪",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(52.5200, 13.4050),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية ألكسندر بلاتز — برلين",
            sampleDescription = "خدمة ليلية متواصلة في وسط برلين."
        ),
        WorldCity(
            id = "es_madrid",
            countryName = "إسبانيا",
            cityName = "مدريد",
            flagEmoji = "🇪🇸",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(40.4168, -3.7038),
            sampleCategory = PinCategory.DEAL,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "عروض سوق سان ميغيل — مدريد",
            sampleDescription = "خصومات مسائية على المأكولات الطازجة."
        ),
        WorldCity(
            id = "es_barcelona",
            countryName = "إسبانيا",
            cityName = "برشلونة",
            flagEmoji = "🇪🇸",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(41.3874, 2.1686),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى لاس رامبلاس — برشلونة",
            sampleDescription = "جلسات مفتوحة وإنترنت مجاني للزوار."
        ),
        WorldCity(
            id = "it_rome",
            countryName = "إيطاليا",
            cityName = "روما",
            flagEmoji = "🇮🇹",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(41.9028, 12.4964),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "تراتوريا روما الأصيلة — روما",
            sampleDescription = "باستا وبيتزا على الحطب قرب نافورة تريفي."
        ),
        WorldCity(
            id = "tr_istanbul",
            countryName = "تركيا",
            cityName = "إسطنبول",
            flagEmoji = "🇹🇷",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(41.0082, 28.9784),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.CROWDED,
            sampleTitle = "مقهى البوسفور — إسطنبول",
            sampleDescription = "شاي وقهوة تركية مطلة على مضيق البوسفور."
        ),
        WorldCity(
            id = "nl_amsterdam",
            countryName = "هولندا",
            cityName = "أمستردام",
            flagEmoji = "🇳🇱",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(52.3676, 4.9041),
            sampleCategory = PinCategory.MAINTENANCE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "ورشة صيانة الدراجات المركزية — أمستردام",
            sampleDescription = "إصلاح فوري للدراجات والمركبات الكهربائية."
        ),
        WorldCity(
            id = "at_vienna",
            countryName = "النمسا",
            cityName = "فيينا",
            flagEmoji = "🇦🇹",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(48.2082, 16.3738),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى فيينا الكلاسيكي — فيينا",
            sampleDescription = "حلويات فينّية أصيلة وأجواء هادئة."
        ),
        WorldCity(
            id = "ch_zurich",
            countryName = "سويسرا",
            cityName = "زيورخ",
            flagEmoji = "🇨🇭",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(47.3769, 8.5417),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية باهنهوفشتراسه — زيورخ",
            sampleDescription = "مفتوحة يومياً لخدمة المقيمين والمسافرين."
        ),
        WorldCity(
            id = "ru_moscow",
            countryName = "روسيا",
            cityName = "موسكو",
            flagEmoji = "🇷🇺",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(55.7558, 37.6173),
            sampleCategory = PinCategory.ROAD_REPORT,
            sampleStatus = PinStatus.TRAFFIC_ALERT,
            sampleTitle = "تحديث مروري: الدائري المركزي — موسكو",
            sampleDescription = "كثافة مرورية وقت الذروة، المترو أسرع حالياً."
        ),
        WorldCity(
            id = "gr_athens",
            countryName = "اليونان",
            cityName = "أثينا",
            flagEmoji = "🇬🇷",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(37.9838, 23.7275),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطعم بلاكا المتوسطي — أثينا",
            sampleDescription = "مأكولات متوسطية طازجة بإطلالة تاريخية."
        ),

        // ==================== Asia ====================
        WorldCity(
            id = "jp_tokyo",
            countryName = "اليابان",
            cityName = "طوكيو",
            flagEmoji = "🇯🇵",
            region = WorldRegion.ASIA,
            location = LatLngPoint(35.6762, 139.6503),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطبخ شيبويا السريع — طوكيو",
            sampleDescription = "خدمة فائقة السرعة وأطباق يابانية طازجة."
        ),
        WorldCity(
            id = "kr_seoul",
            countryName = "كوريا الجنوبية",
            cityName = "سول",
            flagEmoji = "🇰🇷",
            region = WorldRegion.ASIA,
            location = LatLngPoint(37.5665, 126.9780),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى ميونغ دونغ — سول",
            sampleDescription = "مشروبات مبتكرة وخدمة عمل 24 ساعة."
        ),
        WorldCity(
            id = "sg_singapore",
            countryName = "سنغافورة",
            cityName = "سنغافورة",
            flagEmoji = "🇸🇬",
            region = WorldRegion.ASIA,
            location = LatLngPoint(1.3521, 103.8198),
            sampleCategory = PinCategory.DEAL,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "عروض مارينا باي — سنغافورة",
            sampleDescription = "خصومات مسائية في مراكز التسوق والمطاعم."
        ),
        WorldCity(
            id = "my_kualalumpur",
            countryName = "ماليزيا",
            cityName = "كوالالمبور",
            flagEmoji = "🇲🇾",
            region = WorldRegion.ASIA,
            location = LatLngPoint(3.1390, 101.6869),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "شارع العرب بوكيت بينتانج — كوالالمبور",
            sampleDescription = "مطاعم حلال متنوعة مفتوحة حتى ساعات الفجر."
        ),
        WorldCity(
            id = "id_jakarta",
            countryName = "إندونيسيا",
            cityName = "جاكرتا",
            flagEmoji = "🇮🇩",
            region = WorldRegion.ASIA,
            location = LatLngPoint(-6.2088, 106.8456),
            sampleCategory = PinCategory.ROAD_REPORT,
            sampleStatus = PinStatus.TRAFFIC_ALERT,
            sampleTitle = "تنبيه مروري: شارع سوديرمان — جاكرتا",
            sampleDescription = "ازدحام مسائي، استخدم طريق النقل السريع."
        ),
        WorldCity(
            id = "th_bangkok",
            countryName = "تايلاند",
            cityName = "بانكوك",
            flagEmoji = "🇹🇭",
            region = WorldRegion.ASIA,
            location = LatLngPoint(13.7563, 100.5018),
            sampleCategory = PinCategory.DEAL,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "سوق سيام الليلي — بانكوك",
            sampleDescription = "عروض يومية على الأطعمة والمنتجات المحلية."
        ),
        WorldCity(
            id = "cn_beijing",
            countryName = "الصين",
            cityName = "بكين",
            flagEmoji = "🇨🇳",
            region = WorldRegion.ASIA,
            location = LatLngPoint(39.9042, 116.4074),
            sampleCategory = PinCategory.MAINTENANCE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مركز شحن وصيانة المركبات — بكين",
            sampleDescription = "شحن سريع للسيارات الكهربائية وفحص فوري."
        ),
        WorldCity(
            id = "cn_shanghai",
            countryName = "الصين",
            cityName = "شنغهاي",
            flagEmoji = "🇨🇳",
            region = WorldRegion.ASIA,
            location = LatLngPoint(31.2304, 121.4737),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى البوند المطل — شنغهاي",
            sampleDescription = "إطلالة ساحرة على نهر هوانغبو وخدمة سريعة."
        ),
        WorldCity(
            id = "in_mumbai",
            countryName = "الهند",
            cityName = "مومباي",
            flagEmoji = "🇮🇳",
            region = WorldRegion.ASIA,
            location = LatLngPoint(19.0760, 72.8777),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية مارين درايف المناوبة — مومباي",
            sampleDescription = "خدمة دوائية متواصلة 24 ساعة."
        ),
        WorldCity(
            id = "in_delhi",
            countryName = "الهند",
            cityName = "نيودلهي",
            flagEmoji = "🇮🇳",
            region = WorldRegion.ASIA,
            location = LatLngPoint(28.6139, 77.2090),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطاعم كونوت بليس — نيودلهي",
            sampleDescription = "أطباق تقليدية وعالمية طازجة."
        ),
        WorldCity(
            id = "ir_tehran",
            countryName = "إيران",
            cityName = "طهران",
            flagEmoji = "🇮🇷",
            region = WorldRegion.ASIA,
            location = LatLngPoint(35.6892, 51.3890),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى وليعصر — طهران",
            sampleDescription = "جلسات هادئة وسط المدينة."
        ),
        WorldCity(
            id = "pk_karachi",
            countryName = "باكستان",
            cityName = "كراتشي",
            flagEmoji = "🇵🇰",
            region = WorldRegion.ASIA,
            location = LatLngPoint(24.8607, 67.0011),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطعم كليفتون الساحلي — كراتشي",
            sampleDescription = "مشويات ومأكولات بحرية طازجة."
        ),

        // ==================== Africa ====================
        WorldCity(
            id = "ke_nairobi",
            countryName = "كينيا",
            cityName = "نيروبي",
            flagEmoji = "🇰🇪",
            region = WorldRegion.AFRICA,
            location = LatLngPoint(-1.2921, 36.8219),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "محمصة القهوة الكينية — نيروبي",
            sampleDescription = "قهوة كينية طازجة محمصة يومياً."
        ),
        WorldCity(
            id = "ng_lagos",
            countryName = "نيجيريا",
            cityName = "لاغوس",
            flagEmoji = "🇳🇬",
            region = WorldRegion.AFRICA,
            location = LatLngPoint(6.5244, 3.3792),
            sampleCategory = PinCategory.ROAD_REPORT,
            sampleStatus = PinStatus.TRAFFIC_ALERT,
            sampleTitle = "تحديث مروري: جسر البر الرئيسي — لاغوس",
            sampleDescription = "تباطؤ مروري باتجاه الجزيرة."
        ),
        WorldCity(
            id = "za_johannesburg",
            countryName = "جنوب أفريقيا",
            cityName = "جوهانسبرغ",
            flagEmoji = "🇿🇦",
            region = WorldRegion.AFRICA,
            location = LatLngPoint(-26.2041, 28.0473),
            sampleCategory = PinCategory.MAINTENANCE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مركز ساندتون لخدمة السيارات — جوهانسبرغ",
            sampleDescription = "فحص شامل وسريع للمركبات."
        ),
        WorldCity(
            id = "za_capetown",
            countryName = "جنوب أفريقيا",
            cityName = "كيب تاون",
            flagEmoji = "🇿🇦",
            region = WorldRegion.AFRICA,
            location = LatLngPoint(-33.9249, 18.4241),
            sampleCategory = PinCategory.DEAL,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "سوق فيكتوريا البحري — كيب تاون",
            sampleDescription = "عروض اليوم على المنتجات البحرية والحرفية."
        ),
        WorldCity(
            id = "et_addis",
            countryName = "إثيوبيا",
            cityName = "أديس أبابا",
            flagEmoji = "🇪🇹",
            region = WorldRegion.AFRICA,
            location = LatLngPoint(9.0320, 38.7469),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "بيت القهوة الإثيوبية — أديس أبابا",
            sampleDescription = "تحضير القهوة الإثيوبية التقليدية طوال اليوم."
        ),
        WorldCity(
            id = "sn_dakar",
            countryName = "السنغال",
            cityName = "داكار",
            flagEmoji = "🇸🇳",
            region = WorldRegion.AFRICA,
            location = LatLngPoint(14.7167, -17.4677),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية بلاتو المناوبة — داكار",
            sampleDescription = "خدمة دوائية متاحة ليلاً ونهاراً."
        ),

        // ==================== North America ====================
        WorldCity(
            id = "us_nyc",
            countryName = "الولايات المتحدة",
            cityName = "نيويورك",
            flagEmoji = "🇺🇸",
            region = WorldRegion.NORTH_AMERICA,
            location = LatLngPoint(40.7128, -74.0060),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية مانهاتن 24 ساعة — نيويورك",
            sampleDescription = "مفتوحة على مدار الساعة قرب ميدتاون."
        ),
        WorldCity(
            id = "us_la",
            countryName = "الولايات المتحدة",
            cityName = "لوس أنجلوس",
            flagEmoji = "🇺🇸",
            region = WorldRegion.NORTH_AMERICA,
            location = LatLngPoint(34.0522, -118.2437),
            sampleCategory = PinCategory.ROAD_REPORT,
            sampleStatus = PinStatus.TRAFFIC_ALERT,
            sampleTitle = "تنبيه الطريق السريع 405 — لوس أنجلوس",
            sampleDescription = "كثافة مرورية مرتفعة في الاتجاه الجنوبي."
        ),
        WorldCity(
            id = "us_chicago",
            countryName = "الولايات المتحدة",
            cityName = "شيكاغو",
            flagEmoji = "🇺🇸",
            region = WorldRegion.NORTH_AMERICA,
            location = LatLngPoint(41.8781, -87.6298),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطعم اللوب — شيكاغو",
            sampleDescription = "أشهر أطباق شيكاغو الساخنة وخدمة سريعة."
        ),
        WorldCity(
            id = "ca_toronto",
            countryName = "كندا",
            cityName = "تورونتو",
            flagEmoji = "🇨🇦",
            region = WorldRegion.NORTH_AMERICA,
            location = LatLngPoint(43.6532, -79.3832),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى وسط المدينة — تورونتو",
            sampleDescription = "أجواء دافئة وإنترنت فائق السرعة."
        ),
        WorldCity(
            id = "ca_montreal",
            countryName = "كندا",
            cityName = "مونتريال",
            flagEmoji = "🇨🇦",
            region = WorldRegion.NORTH_AMERICA,
            location = LatLngPoint(45.5017, -73.5673),
            sampleCategory = PinCategory.DEAL,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "مخبز البلدة القديمة — مونتريال",
            sampleDescription = "عرض مسائي على المخبوزات الطازجة."
        ),
        WorldCity(
            id = "mx_mexico",
            countryName = "المكسيك",
            cityName = "مكسيكو سيتي",
            flagEmoji = "🇲🇽",
            region = WorldRegion.NORTH_AMERICA,
            location = LatLngPoint(19.4326, -99.1332),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطاعم لا كونديسا — مكسيكو سيتي",
            sampleDescription = "تنوع كبير في المأكولات الطازجة حتى منتصف الليل."
        ),

        // ==================== South America ====================
        WorldCity(
            id = "br_saopaulo",
            countryName = "البرازيل",
            cityName = "ساو باولو",
            flagEmoji = "🇧🇷",
            region = WorldRegion.SOUTH_AMERICA,
            location = LatLngPoint(-23.5505, -46.6333),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى باوليستا — ساو باولو",
            sampleDescription = "قهوة برازيلية مختصة في قلب المدينة."
        ),
        WorldCity(
            id = "br_rio",
            countryName = "البرازيل",
            cityName = "ريو دي جانيرو",
            flagEmoji = "🇧🇷",
            region = WorldRegion.SOUTH_AMERICA,
            location = LatLngPoint(-22.9068, -43.1729),
            sampleCategory = PinCategory.DEAL,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "عروض كوباكابانا — ريو دي جانيرو",
            sampleDescription = "خصومات في المقاهي الساحلية والمطاعم."
        ),
        WorldCity(
            id = "ar_buenosaires",
            countryName = "الأرجنتين",
            cityName = "بوينس آيرس",
            flagEmoji = "🇦🇷",
            region = WorldRegion.SOUTH_AMERICA,
            location = LatLngPoint(-34.6037, -58.3816),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطعم باليرمو — بوينس آيرس",
            sampleDescription = "مشويات طازجة وجلسات مسائية مميزة."
        ),
        WorldCity(
            id = "co_bogota",
            countryName = "كولومبيا",
            cityName = "بوغوتا",
            flagEmoji = "🇨🇴",
            region = WorldRegion.SOUTH_AMERICA,
            location = LatLngPoint(4.7110, -74.0721),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "قهوة الأنديز الكولومبية — بوغوتا",
            sampleDescription = "بن كولومبي محمص طازجاً يومياً."
        ),
        WorldCity(
            id = "cl_santiago",
            countryName = "تشيلي",
            cityName = "سانتياغو",
            flagEmoji = "🇨🇱",
            region = WorldRegion.SOUTH_AMERICA,
            location = LatLngPoint(-33.4489, -70.6693),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية بروفيدنسيا المناوبة — سانتياغو",
            sampleDescription = "خدمة دوائية 24 ساعة."
        ),

        // ==================== Oceania ====================
        WorldCity(
            id = "au_sydney",
            countryName = "أستراليا",
            cityName = "سيدني",
            flagEmoji = "🇦🇺",
            region = WorldRegion.OCEANIA,
            location = LatLngPoint(-33.8688, 151.2093),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى ميناء سيدني — سيدني",
            sampleDescription = "إطلالة مباشرة على الميناء ودار الأوبرا."
        ),
        WorldCity(
            id = "au_melbourne",
            countryName = "أستراليا",
            cityName = "ملبورن",
            flagEmoji = "🇦🇺",
            region = WorldRegion.OCEANIA,
            location = LatLngPoint(-37.8136, 144.9631),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "أزقة القهوة والمطاعم — ملبورن",
            sampleDescription = "أفضل المقاهي والمطاعم في قلب ملبورن."
        ),
        WorldCity(
            id = "nz_auckland",
            countryName = "نيوزيلندا",
            cityName = "أوكلاند",
            flagEmoji = "🇳🇿",
            region = WorldRegion.OCEANIA,
            location = LatLngPoint(-36.8485, 174.7633),
            sampleCategory = PinCategory.MAINTENANCE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مركز الخدمة البحري والمركبات — أوكلاند",
            sampleDescription = "صيانة سريعة ومتوفرة طوال الأسبوع."
        ),

        // ==================== Additional Arab & Global Flagship Cities ====================
        WorldCity(
            id = "sa_makkah",
            countryName = "السعودية",
            cityName = "مكة المكرمة",
            flagEmoji = "🇸🇦",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(21.3891, 39.8579),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية الحرم المركزية — مكة المكرمة",
            sampleDescription = "مفتوحة 24 ساعة لخدمة المعتمرين والزوار."
        ),
        WorldCity(
            id = "sa_madinah",
            countryName = "السعودية",
            cityName = "المدينة المنورة",
            flagEmoji = "🇸🇦",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(24.5247, 39.5692),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى قباء المختص — المدينة المنورة",
            sampleDescription = "جلسات هادئة وقهوة طازجة قرب المسجد النبوي."
        ),
        WorldCity(
            id = "sa_dammam",
            countryName = "السعودية",
            cityName = "الدمام",
            flagEmoji = "🇸🇦",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(26.4207, 50.0888),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطاعم الواجهة البحرية — الدمام",
            sampleDescription = "مأكولات بحرية ومشويات بإطلالة على الخليج."
        ),
        WorldCity(
            id = "ae_sharjah",
            countryName = "الإمارات",
            cityName = "الشارقة",
            flagEmoji = "🇦🇪",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(25.3463, 55.4209),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى واجهة المجاز — الشارقة",
            sampleDescription = "جلسات عائلية جميلة مطلة على النافورة."
        ),
        WorldCity(
            id = "iq_erbil",
            countryName = "العراق",
            cityName = "أربيل",
            flagEmoji = "🇮🇶",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(36.1911, 44.0092),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى القلعة التاريخي — أربيل",
            sampleDescription = "شاي وكهوة قرب قلعة أربيل التراثية."
        ),
        WorldCity(
            id = "iq_mosul",
            countryName = "العراق",
            cityName = "الموصل",
            flagEmoji = "🇮🇶",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(36.3489, 43.1577),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطعم دجلة التراثي — الموصل",
            sampleDescription = "أطباق موصلية أصيلة وخدمة سريعة."
        ),
        WorldCity(
            id = "eg_luxor",
            countryName = "مصر",
            cityName = "الأقصر",
            flagEmoji = "🇪🇬",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(25.6872, 32.6396),
            sampleCategory = PinCategory.DEAL,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "سوق الكرنك السياحي — الأقصر",
            sampleDescription = "عروض يومية على الهدايا والمشروبات على كورنيش النيل."
        ),
        WorldCity(
            id = "ma_marrakech",
            countryName = "المغرب",
            cityName = "مراكش",
            flagEmoji = "🇲🇦",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(31.6295, -7.9811),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطعم جامع الفنا — مراكش",
            sampleDescription = "طاجين وشاي بالنعناع في أجواء مراكش الساحرة."
        ),
        WorldCity(
            id = "ma_tangier",
            countryName = "المغرب",
            cityName = "طنجة",
            flagEmoji = "🇲🇦",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(35.7595, -5.8340),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى الحافة المطل — طنجة",
            sampleDescription = "إطلالة خلابة على مضيق جبل طارق."
        ),
        WorldCity(
            id = "dz_oran",
            countryName = "الجزائر",
            cityName = "وهران",
            flagEmoji = "🇩🇿",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(35.6977, -0.6308),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى واجهة البحر — وهران",
            sampleDescription = "إطلالة متوسطية وجلسات مسائية مريحة."
        ),
        WorldCity(
            id = "sy_aleppo",
            countryName = "سوريا",
            cityName = "حلب",
            flagEmoji = "🇸🇾",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(36.2021, 37.1343),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطعم ومشويات القلعة — حلب",
            sampleDescription = "مأكولات حلبية تقليدية طازجة."
        ),
        WorldCity(
            id = "ps_gaza",
            countryName = "فلسطين",
            cityName = "غزة",
            flagEmoji = "🇵🇸",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(31.5017, 34.4668),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "نقطة خدمات طبية ومناوبة — غزة",
            sampleDescription = "تحديث حي للخدمات المتوفرة والمراكز القريبة."
        ),
        WorldCity(
            id = "ly_benghazi",
            countryName = "ليبيا",
            cityName = "بنغازي",
            flagEmoji = "🇱🇾",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(32.1194, 20.0868),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية الفويهات المناوبة — بنغازي",
            sampleDescription = "خدمة دوائية متوفرة على مدار الساعة."
        ),
        WorldCity(
            id = "om_salalah",
            countryName = "عُمان",
            cityName = "صلالة",
            flagEmoji = "🇴🇲",
            region = WorldRegion.ARAB_WORLD,
            location = LatLngPoint(17.0151, 54.0924),
            sampleCategory = PinCategory.DEAL,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "سوق الحافة للعطور واللبان — صلالة",
            sampleDescription = "عروض موسمية على اللبان العماني."
        ),
        WorldCity(
            id = "tr_ankara",
            countryName = "تركيا",
            cityName = "أنقرة",
            flagEmoji = "🇹🇷",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(39.9334, 32.8597),
            sampleCategory = PinCategory.PHARMACY,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "صيدلية كيزيلاي المناوبة — أنقرة",
            sampleDescription = "مفتوحة 24 ساعة في مركز أنقرة."
        ),
        WorldCity(
            id = "it_milan",
            countryName = "إيطاليا",
            cityName = "ميلانو",
            flagEmoji = "🇮🇹",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(45.4642, 9.1900),
            sampleCategory = PinCategory.DEAL,
            sampleStatus = PinStatus.DEAL_ACTIVE,
            sampleTitle = "عروض ساحة الدومو — ميلانو",
            sampleDescription = "خصومات في المقاهي والمتاجر المركزية."
        ),
        WorldCity(
            id = "pt_lisbon",
            countryName = "البرتغال",
            cityName = "لشبونة",
            flagEmoji = "🇵🇹",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(38.7223, -9.1393),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى ومخبز بيليم — لشبونة",
            sampleDescription = "معجنات برتغالية طازجة وقهوة مختصة."
        ),
        WorldCity(
            id = "cz_prague",
            countryName = "التشيك",
            cityName = "براغ",
            flagEmoji = "🇨🇿",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(50.0755, 14.4378),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى البلدة القديمة — براغ",
            sampleDescription = "جلسات دافئة قرب الساعة الفلكية."
        ),
        WorldCity(
            id = "se_stockholm",
            countryName = "السويد",
            cityName = "ستوكهولم",
            flagEmoji = "🇸🇪",
            region = WorldRegion.EUROPE,
            location = LatLngPoint(59.3293, 18.0686),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى غاملا ستان — ستوكهولم",
            sampleDescription = "قهوة سويدية وحلويات القرفة الطازجة."
        ),
        WorldCity(
            id = "jp_osaka",
            countryName = "اليابان",
            cityName = "أوساكا",
            flagEmoji = "🇯🇵",
            region = WorldRegion.ASIA,
            location = LatLngPoint(34.6937, 135.5023),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطبخ دوتونبوري الحي — أوساكا",
            sampleDescription = "مأكولات سريعة مفتوحة حتى ساعات الفجر."
        ),
        WorldCity(
            id = "id_bali",
            countryName = "إندونيسيا",
            cityName = "بالي",
            flagEmoji = "🇮🇩",
            region = WorldRegion.ASIA,
            location = LatLngPoint(-8.4095, 115.1889),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى أوبود الاستوائي — بالي",
            sampleDescription = "إنترنت فائق السرعة وإطلالة على الطبيعة."
        ),
        WorldCity(
            id = "us_miami",
            countryName = "الولايات المتحدة",
            cityName = "ميامي",
            flagEmoji = "🇺🇸",
            region = WorldRegion.NORTH_AMERICA,
            location = LatLngPoint(25.7617, -80.1918),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطعم ساوث بيتش البحري — ميامي",
            sampleDescription = "أجواء ساحلية ومأكولات طازجة."
        ),
        WorldCity(
            id = "us_dc",
            countryName = "الولايات المتحدة",
            cityName = "واشنطن",
            flagEmoji = "🇺🇸",
            region = WorldRegion.NORTH_AMERICA,
            location = LatLngPoint(38.9072, -77.0369),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى جورجتاون — واشنطن",
            sampleDescription = "قهوة مختصة وجلسات هادئة للدراسة."
        ),
        WorldCity(
            id = "ca_vancouver",
            countryName = "كندا",
            cityName = "فانكوفر",
            flagEmoji = "🇨🇦",
            region = WorldRegion.NORTH_AMERICA,
            location = LatLngPoint(49.2827, -123.1207),
            sampleCategory = PinCategory.MAINTENANCE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مركز خدمة المركبات الكهربائية — فانكوفر",
            sampleDescription = "شحن وصيانة سريعة."
        ),
        WorldCity(
            id = "pe_lima",
            countryName = "بيرو",
            cityName = "ليما",
            flagEmoji = "🇵🇪",
            region = WorldRegion.SOUTH_AMERICA,
            location = LatLngPoint(-12.0464, -77.0428),
            sampleCategory = PinCategory.RESTAURANT,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مطاعم ميرافلوريس — ليما",
            sampleDescription = "أطباق بحرية طازجة مطلة على المحيط."
        ),
        WorldCity(
            id = "au_perth",
            countryName = "أستراليا",
            cityName = "بيرث",
            flagEmoji = "🇦🇺",
            region = WorldRegion.OCEANIA,
            location = LatLngPoint(-31.9505, 115.8605),
            sampleCategory = PinCategory.CAFE,
            sampleStatus = PinStatus.OPEN,
            sampleTitle = "مقهى نهر سوان — بيرث",
            sampleDescription = "جلسات صباحية ومسائية هادئة."
        )
    )
}
