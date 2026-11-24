package com.example.model

enum class HubKind {
    AIRPORT,
    SEAPORT
}

data class TransportHub(
    val id: String,
    val nameAr: String,
    val cityAr: String,
    val kind: HubKind,
    val location: LatLngPoint
)

object TransportHubCatalog {

    val airports: List<TransportHub> = listOf(
        // Saudi Arabia & Gulf
        TransportHub("ruh", "مطار الملك خالد الدولي", "الرياض", HubKind.AIRPORT, LatLngPoint(24.9576, 46.6988)),
        TransportHub("jed", "مطار الملك عبدالعزيز الدولي", "جدة", HubKind.AIRPORT, LatLngPoint(21.6796, 39.1565)),
        TransportHub("dmm", "مطار الملك فهد الدولي", "الدمام", HubKind.AIRPORT, LatLngPoint(26.4712, 49.7979)),
        TransportHub("med", "مطار الأمير محمد بن عبدالعزيز", "المدينة المنورة", HubKind.AIRPORT, LatLngPoint(24.5534, 39.7051)),
        TransportHub("ahb", "مطار أبها الدولي", "أبها", HubKind.AIRPORT, LatLngPoint(18.2404, 42.6566)),
        TransportHub("dxb", "مطار دبي الدولي", "دبي", HubKind.AIRPORT, LatLngPoint(25.2532, 55.3657)),
        TransportHub("auh", "مطار زايد الدولي", "أبوظبي", HubKind.AIRPORT, LatLngPoint(24.4330, 54.6511)),
        TransportHub("doh", "مطار حمد الدولي", "الدوحة", HubKind.AIRPORT, LatLngPoint(25.2731, 51.6081)),
        TransportHub("kwi", "مطار الكويت الدولي", "الكويت", HubKind.AIRPORT, LatLngPoint(29.2266, 47.9689)),
        TransportHub("bah", "مطار البحرين الدولي", "المنامة", HubKind.AIRPORT, LatLngPoint(26.2708, 50.6336)),
        TransportHub("mct", "مطار مسقط الدولي", "مسقط", HubKind.AIRPORT, LatLngPoint(23.5933, 58.2844)),
        TransportHub("sll", "مطار صلالة الدولي", "صلالة", HubKind.AIRPORT, LatLngPoint(17.0387, 54.0913)),

        // Iraq, Levant & Yemen
        TransportHub("bgw", "مطار بغداد الدولي", "بغداد", HubKind.AIRPORT, LatLngPoint(33.2625, 44.2346)),
        TransportHub("bsr", "مطار البصرة الدولي", "البصرة", HubKind.AIRPORT, LatLngPoint(30.5491, 47.6621)),
        TransportHub("ebl", "مطار أربيل الدولي", "أربيل", HubKind.AIRPORT, LatLngPoint(36.2376, 43.9632)),
        TransportHub("njf", "مطار النجف الأشرف الدولي", "النجف", HubKind.AIRPORT, LatLngPoint(31.9897, 44.4043)),
        TransportHub("amm", "مطار الملكة علياء الدولي", "عمّان", HubKind.AIRPORT, LatLngPoint(31.7226, 35.9932)),
        TransportHub("bey", "مطار رفيق الحريري الدولي", "بيروت", HubKind.AIRPORT, LatLngPoint(33.8209, 35.4884)),
        TransportHub("dam", "مطار دمشق الدولي", "دمشق", HubKind.AIRPORT, LatLngPoint(33.4115, 36.5156)),
        TransportHub("ade", "مطار عدن الدولي", "عدن", HubKind.AIRPORT, LatLngPoint(12.8295, 45.0288)),
        TransportHub("sah", "مطار صنعاء الدولي", "صنعاء", HubKind.AIRPORT, LatLngPoint(15.4763, 44.2197)),

        // Egypt, Sudan & North/East Africa
        TransportHub("cai", "مطار القاهرة الدولي", "القاهرة", HubKind.AIRPORT, LatLngPoint(30.1219, 31.4056)),
        TransportHub("hbe", "مطار برج العرب الدولي", "الإسكندرية", HubKind.AIRPORT, LatLngPoint(30.9177, 29.6964)),
        TransportHub("ssh", "مطار شرم الشيخ الدولي", "شرم الشيخ", HubKind.AIRPORT, LatLngPoint(27.9773, 34.3950)),
        TransportHub("hrg", "مطار الغردقة الدولي", "الغردقة", HubKind.AIRPORT, LatLngPoint(27.1783, 33.7994)),
        TransportHub("krt", "مطار الخرطوم الدولي", "الخرطوم", HubKind.AIRPORT, LatLngPoint(15.5895, 32.5532)),
        TransportHub("tip", "مطار معيتيقة الدولي", "طرابلس", HubKind.AIRPORT, LatLngPoint(32.8941, 13.2760)),
        TransportHub("tun", "مطار تونس قرطاج الدولي", "تونس", HubKind.AIRPORT, LatLngPoint(36.8510, 10.2272)),
        TransportHub("alg", "مطار هواري بومدين الدولي", "الجزائر", HubKind.AIRPORT, LatLngPoint(36.6910, 3.2154)),
        TransportHub("cmn", "مطار محمد الخامس الدولي", "الدار البيضاء", HubKind.AIRPORT, LatLngPoint(33.3675, -7.5899)),
        TransportHub("tng", "مطار طنجة ابن بطوطة", "طنجة", HubKind.AIRPORT, LatLngPoint(35.7269, -5.9169)),
        TransportHub("nkc", "مطار نواكشوط أم التونسي", "نواكشوط", HubKind.AIRPORT, LatLngPoint(18.3100, -15.9697)),
        TransportHub("jib", "مطار جيبوتي أمبولي الدولي", "جيبوتي", HubKind.AIRPORT, LatLngPoint(11.5473, 43.1595)),
        TransportHub("mgq", "مطار آدم عدي الدولي", "مقديشو", HubKind.AIRPORT, LatLngPoint(2.0144, 45.3047)),
        TransportHub("nbo", "مطار جومو كينياتا الدولي", "نيروبي", HubKind.AIRPORT, LatLngPoint(-1.3192, 36.9278)),
        TransportHub("add", "مطار أديس أبابا بولي الدولي", "أديس أبابا", HubKind.AIRPORT, LatLngPoint(8.9779, 38.7993)),
        TransportHub("los", "مطار مورتالا محمد الدولي", "لاغوس", HubKind.AIRPORT, LatLngPoint(6.5774, 3.3212)),
        TransportHub("jnb", "مطار أوليفر تامبو الدولي", "جوهانسبرغ", HubKind.AIRPORT, LatLngPoint(-26.1392, 28.2460)),

        // Europe & Turkey
        TransportHub("ist", "مطار إسطنبول الدولي", "إسطنبول", HubKind.AIRPORT, LatLngPoint(41.2753, 28.7519)),
        TransportHub("esb", "مطار أنقرة إيسنبوغا", "أنقرة", HubKind.AIRPORT, LatLngPoint(40.1281, 32.9951)),
        TransportHub("lhr", "مطار لندن هيثرو الدولي", "لندن", HubKind.AIRPORT, LatLngPoint(51.4700, -0.4543)),
        TransportHub("cdg", "مطار باريس شارل ديغول", "باريس", HubKind.AIRPORT, LatLngPoint(49.0097, 2.5479)),
        TransportHub("mad", "مطار مدريد باراخاس الدولي", "مدريد", HubKind.AIRPORT, LatLngPoint(40.4983, -3.5676)),
        TransportHub("fco", "مطار روما فيوميتشينو", "روما", HubKind.AIRPORT, LatLngPoint(41.8003, 12.2389)),
        TransportHub("fra", "مطار فرانكفورت الدولي", "فرانكفورت", HubKind.AIRPORT, LatLngPoint(50.0379, 8.5622)),
        TransportHub("ams", "مطار أمستردام سخيبول", "أمستردام", HubKind.AIRPORT, LatLngPoint(52.3105, 4.7683)),
        TransportHub("ath", "مطار أثينا الدولي", "أثينا", HubKind.AIRPORT, LatLngPoint(37.9364, 23.9445)),
        TransportHub("svo", "مطار موسكو شيريميتيفو", "موسكو", HubKind.AIRPORT, LatLngPoint(55.9726, 37.4146)),

        // Asia, Americas & Oceania
        TransportHub("bom", "مطار مومباي الدولي", "مومباي", HubKind.AIRPORT, LatLngPoint(19.0896, 72.8656)),
        TransportHub("del", "مطار نيودلهي الدولي", "نيودلهي", HubKind.AIRPORT, LatLngPoint(28.5562, 77.1000)),
        TransportHub("khi", "مطار جناح الدولي", "كراتشي", HubKind.AIRPORT, LatLngPoint(24.9065, 67.1608)),
        TransportHub("bkk", "مطار بانكوك سوفارنابومي", "بانكوك", HubKind.AIRPORT, LatLngPoint(13.6900, 100.7501)),
        TransportHub("kul", "مطار كوالالمبور الدولي", "كوالالمبور", HubKind.AIRPORT, LatLngPoint(2.7456, 101.7099)),
        TransportHub("cgk", "مطار سوكارنو هاتا الدولي", "جاكرتا", HubKind.AIRPORT, LatLngPoint(-6.1275, 106.6537)),
        TransportHub("sin", "مطار سنغافورة شانغي", "سنغافورة", HubKind.AIRPORT, LatLngPoint(1.3644, 103.9915)),
        TransportHub("pek", "مطار بكين العاصمة الدولي", "بكين", HubKind.AIRPORT, LatLngPoint(40.0799, 116.6031)),
        TransportHub("icn", "مطار سيول إنشون الدولي", "سيول", HubKind.AIRPORT, LatLngPoint(37.4602, 126.4407)),
        TransportHub("hnd", "مطار طوكيو هانيدا الدولي", "طوكيو", HubKind.AIRPORT, LatLngPoint(35.5494, 139.7798)),
        TransportHub("jfk", "مطار جون كينيدي الدولي", "نيويورك", HubKind.AIRPORT, LatLngPoint(40.6413, -73.7781)),
        TransportHub("lax", "مطار لوس أنجلوس الدولي", "لوس أنجلوس", HubKind.AIRPORT, LatLngPoint(33.9416, -118.4085)),
        TransportHub("yyz", "مطار تورونتو بيرسون", "تورونتو", HubKind.AIRPORT, LatLngPoint(43.6777, -79.6248)),
        TransportHub("gru", "مطار ساو باولو غواروليوس", "ساو باولو", HubKind.AIRPORT, LatLngPoint(-23.4356, -46.4731)),
        TransportHub("eze", "مطار بوينس آيرس إيزيزا", "بوينس آيرس", HubKind.AIRPORT, LatLngPoint(-34.8222, -58.5358)),
        TransportHub("syd", "مطار سيدني كينغسفورد سميث", "سيدني", HubKind.AIRPORT, LatLngPoint(-33.9399, 151.1753))
    )

    val seaports: List<TransportHub> = listOf(
        // Red Sea, Arabian Gulf & Mediterranean Arab Ports
        TransportHub("p_jed", "ميناء جدة الإسلامي", "جدة", HubKind.SEAPORT, LatLngPoint(21.4833, 39.1520)),
        TransportHub("p_duba", "ميناء ضباء البحري", "ضباء", HubKind.SEAPORT, LatLngPoint(27.3492, 35.6945)),
        TransportHub("p_yanbu", "ميناء ينبع البحري", "ينبع", HubKind.SEAPORT, LatLngPoint(24.0835, 38.0637)),
        TransportHub("p_dmm", "ميناء الملك عبدالعزيز بالدمام", "الدمام", HubKind.SEAPORT, LatLngPoint(26.4945, 50.1947)),
        TransportHub("p_dxb", "ميناء راشد البحري", "دبي", HubKind.SEAPORT, LatLngPoint(25.2722, 55.2762)),
        TransportHub("p_auh", "ميناء زايد البحري", "أبوظبي", HubKind.SEAPORT, LatLngPoint(24.5194, 54.3822)),
        TransportHub("p_doh", "ميناء الدوحة البحري", "الدوحة", HubKind.SEAPORT, LatLngPoint(25.2917, 51.5476)),
        TransportHub("p_bah", "ميناء خليفة بن سلمان", "المنامة", HubKind.SEAPORT, LatLngPoint(26.1852, 50.6908)),
        TransportHub("p_kwi", "ميناء الشويخ البحري", "الكويت", HubKind.SEAPORT, LatLngPoint(29.3547, 47.9261)),
        TransportHub("p_mct", "ميناء السلطان قابوس", "مسقط", HubKind.SEAPORT, LatLngPoint(23.6272, 58.5675)),
        TransportHub("p_sll", "ميناء صلالة البحري", "صلالة", HubKind.SEAPORT, LatLngPoint(16.9383, 54.0046)),
        TransportHub("p_bsr", "ميناء الفاو وأم قصر", "البصرة", HubKind.SEAPORT, LatLngPoint(30.0230, 47.9430)),
        TransportHub("p_aqj", "ميناء العقبة البحري", "العقبة", HubKind.SEAPORT, LatLngPoint(29.5130, 34.9960)),
        TransportHub("p_saf", "ميناء سفاجا البحري", "سفاجا", HubKind.SEAPORT, LatLngPoint(26.7421, 33.9410)),
        TransportHub("p_hrg", "ميناء الغردقة البحري", "الغردقة", HubKind.SEAPORT, LatLngPoint(27.2255, 33.8442)),
        TransportHub("p_ssh", "ميناء شرم الشيخ البحري", "شرم الشيخ", HubKind.SEAPORT, LatLngPoint(27.8555, 34.2877)),
        TransportHub("p_alx", "ميناء الإسكندرية البحري", "الإسكندرية", HubKind.SEAPORT, LatLngPoint(31.1927, 29.8736)),
        TransportHub("p_psd", "ميناء بورسعيد البحري", "بورسعيد", HubKind.SEAPORT, LatLngPoint(31.2636, 32.3133)),
        TransportHub("p_bey", "مرفأ بيروت البحري", "بيروت", HubKind.SEAPORT, LatLngPoint(33.9022, 35.5150)),
        TransportHub("p_psu", "ميناء بورتسودان البحري", "بورتسودان", HubKind.SEAPORT, LatLngPoint(19.6158, 37.2164)),
        TransportHub("p_jib", "ميناء جيبوتي البحري", "جيبوتي", HubKind.SEAPORT, LatLngPoint(11.6020, 43.1360)),
        TransportHub("p_tip", "ميناء طرابلس البحري", "طرابلس", HubKind.SEAPORT, LatLngPoint(32.9012, 13.1920)),
        TransportHub("p_tun", "ميناء حلق الوادي", "تونس", HubKind.SEAPORT, LatLngPoint(36.8152, 10.3055)),
        TransportHub("p_alg", "ميناء الجزائر البحري", "الجزائر", HubKind.SEAPORT, LatLngPoint(36.7745, 3.0650)),
        TransportHub("p_tng", "ميناء طنجة المتوسط", "طنجة", HubKind.SEAPORT, LatLngPoint(35.8878, -5.5050)),
        TransportHub("p_cas", "ميناء الدار البيضاء البحري", "الدار البيضاء", HubKind.SEAPORT, LatLngPoint(33.6061, -7.6140)),

        // Global Major Seaports
        TransportHub("p_ist", "ميناء إسطنبول البحري", "إسطنبول", HubKind.SEAPORT, LatLngPoint(41.0080, 28.9850)),
        TransportHub("p_pir", "ميناء بيرايوس البحري", "أثينا", HubKind.SEAPORT, LatLngPoint(37.9420, 23.6350)),
        TransportHub("p_bcn", "ميناء برشلونة البحري", "برشلونة", HubKind.SEAPORT, LatLngPoint(41.3680, 2.1740)),
        TransportHub("p_mrs", "ميناء مرسيليا البحري", "مرسيليا", HubKind.SEAPORT, LatLngPoint(43.3180, 5.3520)),
        TransportHub("p_dov", "ميناء دوفر البحري", "دوفر", HubKind.SEAPORT, LatLngPoint(51.1270, 1.3310)),
        TransportHub("p_rtm", "ميناء روتردام البحري", "روتردام", HubKind.SEAPORT, LatLngPoint(51.9080, 4.4600)),
        TransportHub("p_bom", "ميناء مومباي البحري", "مومباي", HubKind.SEAPORT, LatLngPoint(18.9480, 72.8430)),
        TransportHub("p_sin", "ميناء سنغافورة البحري", "سنغافورة", HubKind.SEAPORT, LatLngPoint(1.2644, 103.8220)),
        TransportHub("p_tyo", "ميناء طوكيو البحري", "طوكيو", HubKind.SEAPORT, LatLngPoint(35.6230, 139.7760)),
        TransportHub("p_nyc", "ميناء نيويورك البحري", "نيويورك", HubKind.SEAPORT, LatLngPoint(40.6840, -74.0120)),
        TransportHub("p_rio", "ميناء ريو دي جانيرو البحري", "ريو دي جانيرو", HubKind.SEAPORT, LatLngPoint(-22.8930, -43.1840)),
        TransportHub("p_syd", "ميناء سيدني البحري", "سيدني", HubKind.SEAPORT, LatLngPoint(-33.8560, 151.2090))
    )

    fun nearestAirport(point: LatLngPoint): TransportHub =
        airports.minBy {
            calculateDistanceMeters(point.latitude, point.longitude, it.location.latitude, it.location.longitude)
        }

    fun nearestSeaport(point: LatLngPoint): TransportHub =
        seaports.minBy {
            calculateDistanceMeters(point.latitude, point.longitude, it.location.latitude, it.location.longitude)
        }
}
