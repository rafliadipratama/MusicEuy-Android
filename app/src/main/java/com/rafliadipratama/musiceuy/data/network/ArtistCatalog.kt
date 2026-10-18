package com.rafliadipratama.musiceuy.data.network

import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track

object ArtistCatalog {

    /**
     * Master verified avatar directory for popular Indonesian and Western artists.
     * Guarantees 100% high-resolution (500x500) avatars even when offline or newly played.
     */
    private val MASTER_ARTIST_AVATARS = mapOf(
        // Top Indonesian Artists (100% Official YouTube Music Google CDN)
        "sheila on 7" to "https://lh3.googleusercontent.com/ZBI5XEhdUvkE59Kr7HKZYIjoopuA_kC0uPAWNsEw-EmLfTtRaXuDEv-qYWTv_lahkpA8Yuhe7P-sO2Y=w544-h544-l90-rj",
        "bernadya" to "https://lh3.googleusercontent.com/hxROE1fvLWSxUYAFV3IgMp5vvjN91Jx6oS6uwSVyYN9_fb3I6PLaRa3Ufb4A0awxq4J5UoPFQAQM-Q=w544-h544-l90-rj",
        "hindia" to "https://yt3.googleusercontent.com/8ImMAMQSD4FA6-gdqCZWSFaB-drHvkdfiFcFAk7Mcyy56ctfWD-Xxno-CHfGC4L6Ql8aR61XT0vX0F4b=w544-h544-l90-rj",
        "tulus" to "https://yt3.googleusercontent.com/h8P1jEIZLM8lkMxNA6Nbq98b43wcqllJSNmcZTCRPAB-F6rG_0Nqw5w7fwou0PN1QGwSW5viwWD5NV0=w544-h544-l90-rj",
        "mahalini" to "https://yt3.googleusercontent.com/VdgLqr3Sno_U1IXj9qzk43azloCsjBeDy6MpFjfD8kMmco0AeL81qow0cpHynDfpaVlujCY11O7QO4d6=w544-h544-l90-rj",
        "sal priadi" to "https://lh3.googleusercontent.com/wmItRT4hTCJrmnlsh_JBgOeBXww9mquXhrNR0oW3_hPW9LsZ5Z2grMij01EaENdt6ensOJfKm-OCBKqJ=w544-h544-l90-rj",
        "niki" to "https://yt3.googleusercontent.com/gKAVVgmPq8rRVjGXzkiaSxnHjfBdOBbl3kf1Nz2cel7vobVD5yzSVPE-Xr4c3brFq2nZ9xbbloRbPg=w544-h544-l90-rj",
        "juicy luicy" to "https://yt3.googleusercontent.com/DDebW5VciXI_oMRQC1cRIWlpDIWVaS8c_CbcCkf89YeHVziP9lkgA0xUZmmVRxGKmC3qmppuMtvIsa5U=w544-h544-l90-rj",
        "nadin amizah" to "https://yt3.googleusercontent.com/1MrTMYk3XQeTy0EDkJOOkRaHwV2jo-8Es2y8ksOyDThn1btv3VrtzRJy8PbRzVY1V-xj9DBaELpR8jnS=w544-h544-l90-rj",
        "dewa 19" to "https://yt3.googleusercontent.com/xFQbHjuH9LO_HkIMSAOf-vu1jMe-hH8y0L0VVEKPSkQn650JFu1Y46RWIIE4oF_kt0JTR5DlxI-NJwBc=w544-h544-l90-rj",
        "noah" to "https://lh3.googleusercontent.com/6xEP8mOA5xguCiwfeNq08D1o0GK7iPSNMwp80jJpyxw4bwdOgo4EO-8T4rnb8gv3_283YEoQsZvRLD4k=w544-h544-l90-rj",
        "peterpan" to "https://lh3.googleusercontent.com/6xEP8mOA5xguCiwfeNq08D1o0GK7iPSNMwp80jJpyxw4bwdOgo4EO-8T4rnb8gv3_283YEoQsZvRLD4k=w544-h544-l90-rj",
        "lyodra" to "https://yt3.googleusercontent.com/fX1X2u4D_p36NXWXqmOJneksx8UuAC4YaZGU4IKShOMX4NtaA3CCKyMoFDh4B3WHEIjL6fg7Z4zwb0s=w544-h544-l90-rj",
        "tiara andini" to "https://lh3.googleusercontent.com/gYD0RV885nA7hEE9gMEZyiB-aqbiOugfsXaVChO4zBXtOqzKsiiVFGBZin01zDizrp_LeBELRrSuRw=w544-h544-l90-rj",
        "denny caknan" to "https://lh3.googleusercontent.com/woWOuYcLDhSRRyFn6kNHJtaiGpD3dW2SnYZanXVdqe96T9Qs1Bt1GchfV73HsIJq2R013krAX3DyLA=w544-h544-l90-rj",
        "guyon waton" to "https://yt3.googleusercontent.com/BeY4sDpZ9zge0jFVH0s7v31UQoOb3FubZXQGQ3z_zgFraWot-MkUuF7JH3sSCjtEfzwDgjs3LDgnVsyDJg=w544-h544-l90-rj",
        "feby putri" to "https://yt3.googleusercontent.com/BOaqGQqwh7TOLyGlLJI3QcIBezoMHpSHHN5Kn73bbvlqjhc1-hS4mDRYLs_N_yjR81RWrNi-ojwFlw=w544-h544-l90-rj",
        "pamungkas" to "https://yt3.googleusercontent.com/taxtgIghV9GbhxPOJ2_Utqh8T72EEBcGynoNX2dw9z-sSurU2GIW7vEM06ZYHJLOtcIOXvQZTpkfoJc=w544-h544-l90-rj",
        "reality club" to "https://lh3.googleusercontent.com/YfD3p4VViGZEJw-d5IrbyTjj7YFfd1LnpLP6BU2hFm0WGbHM25ot2BonJZHNV6CeGXjJ_zI5OZVXXyW0=w544-h544-l90-rj",
        "fourtwnty" to "https://yt3.googleusercontent.com/2o9I4mMwti1cH7nILSQ4nu3X7f7scfSndSOKTqaJ7FGLo3bGLyXSadZOBIT6bSgbc6H8zYb_gQ=w544-h544-l90-rj",
        "padi" to "https://yt3.ggpht.com/ytc/AIdro_l5JpQiYL1g25UijiewngWtu8o4imIch4x-YMFle1IY_Q=w544-h544-l90-rj",
        "raisa" to "https://lh3.googleusercontent.com/HsZDqggC4CwKO3Jp3svqgLquXHRaMENJgx35ADX9ecZoozfxXSr1FgOOBEDIixwwsFWBTqpI1JLNvrho=w544-h544-l90-rj",
        "afgan" to "https://yt3.googleusercontent.com/K5YkN0ruZFb_aqz2caPgencgflSqp0BqJYOSEhaCfVcjodcQS65vfH6_IO6JJUnaHI3D2s8v3i8AEyo=w544-h544-l90-rj",
        "rizky febian" to "https://yt3.googleusercontent.com/uPbJD1rupFLyxWEOdwV7oO5joK_HbMKin5XsUR_hg1z0WZGp-u9_KtD6sF6X63qkdUZjkXXwabqmik_n=w544-h544-l90-rj",
        "idgitaf" to "https://yt3.googleusercontent.com/UjGi4OiorRoR53C60a5AlpFgjno4czBHDD9b9GZXyVlhliSsA5ImOoLN9o2urdnGVmZ0hgkxQKAZkyU=w544-h544-l90-rj",
        "fiersa besari" to "https://yt3.googleusercontent.com/uRhP7xgP0ceSFvwo3NP4sVL_iW1hn-NAdbmzl2i_iGvE0-8dwxWR6rhc5iFtoLUYK8EIFayp=w544-h544-l90-rj",
        "eñau" to "https://lh3.googleusercontent.com/5_t9JRn7KopQJjyw8_1t8RFmaNSaX-P75wsHbUGDNqTNS3X-qUDvU8033AJn3D_y1IK-gSpIzBwX4w=w544-h544-l90-rj",
        "enau" to "https://lh3.googleusercontent.com/5_t9JRn7KopQJjyw8_1t8RFmaNSaX-P75wsHbUGDNqTNS3X-qUDvU8033AJn3D_y1IK-gSpIzBwX4w=w544-h544-l90-rj",
        ".feast" to "https://lh3.googleusercontent.com/v78O_zDgEMvUp35uR0s0fU9pyvRJkELYJKKfYS33Kg1NpQrE4W-dzuS3CHuwWTdYsIs5RzwNpkiekcE=w544-h544-l90-rj",
        "feast" to "https://lh3.googleusercontent.com/v78O_zDgEMvUp35uR0s0fU9pyvRJkELYJKKfYS33Kg1NpQrE4W-dzuS3CHuwWTdYsIs5RzwNpkiekcE=w544-h544-l90-rj",
        "ran" to "https://lh3.googleusercontent.com/Qg2Ai5oRQQkmcAkUsAuYtK3RUwqNUHMtEVwovMlcIN9VZdNhwkUaqSy6kKHYW9fhzsx1QvhZ2iW7Gg=w544-h544-l90-rj",
        "maman fvndy" to "https://yt3.googleusercontent.com/eOf6QDdK2cP98WYKsbJg_BhvdbP1zoxIAmQTHquiUbwzPGBC5Ci0R8JV0mdKlpAMtIP4XeN9b7g=w544-h544-l90-rj",
        "payung teduh" to "https://yt3.googleusercontent.com/QevM0AhNvLh0cPNeCRfyVToHCso_gCyOMlRMElvJXbx8j8BhzjEutfJhaBbDhJy_ebLOqsOOb5o=w544-h544-l90-rj",
        "danilla" to "https://lh3.googleusercontent.com/TjGBboAUulSaDK48_nyOYQWm_CzSO792iDBmFPj5Bfo9fOdR4TxUTcLcrEOt_oeOKzTvzjkZTiK_qg=w544-h544-l90-rj",
        "ndx a.k.a" to "https://yt3.googleusercontent.com/MyfVUOKWHpCifDs7cDKeoLuzJNnC4zicErRVFub3yprKe9xc4ncdlSOGlXNYbrqqDk8yKhoFFPL3x_T0=w544-h544-l90-rj",
        "ndx aka" to "https://yt3.googleusercontent.com/MyfVUOKWHpCifDs7cDKeoLuzJNnC4zicErRVFub3yprKe9xc4ncdlSOGlXNYbrqqDk8yKhoFFPL3x_T0=w544-h544-l90-rj",

        // Top Western & Global Artists (100% Official YouTube Music Google CDN)
        "radiohead" to "https://lh3.googleusercontent.com/G-Vsaq4pw6Bl5Ny4p_wV9obzu_eyZxynQycvCvQru4Wglzfxg4NO9owDKnSlKys_-WRzPEc0O6ydGA=w544-h544-l90-rj",
        "michael jackson" to "https://lh3.googleusercontent.com/x-Z35q6HsBB98J85-4oNqPnOen4pZBaNCpHawzf_ejs-pkgh6Eh3D2Fu7S1T4gEj0yWZ0c6DOAHCpA=w544-h544-l90-rj",
        "bruno mars" to "https://lh3.googleusercontent.com/hnefGBrazRhn4Z92bdSZBUENl40ONjRiVDsmZKZh-WZ2iCKE-2c7KKR7SNcZfzLHoRyB3E6as8L87YA=w544-h544-l90-rj",
        "taylor swift" to "https://yt3.googleusercontent.com/RCpTA6EXJQyjVFDosWOKa2SMmqkua_lA9mHPDWWciLwgqpZLz-k8rXWRF_367trrQ7up9BUwCbk6kRk=w544-h544-l90-rj",
        "billie eilish" to "https://lh3.googleusercontent.com/tQC4rOL6xz6FhmFr0ggQExxyGbYSOsyveXVSnPBh2WjEyIzQ9pMHablLJ-0GlMBrLBlBrbWQGmzrV6KN=w544-h544-l90-rj",
        "coldplay" to "https://lh3.googleusercontent.com/IOKuXtp8PCQ_Fc-vaRKm3sKIXBxFV51gZheLTH5br-YGnWHFQf_Jywcuk7wbprYRoEbQyS_XZY6-nMJX=w544-h544-l90-rj",
        "bleachers" to "https://lh3.googleusercontent.com/6v1HcN3P8VBOENfjZT5sHnkyp17yas1plImu6gKcGmSi2B7CqtLYRXN7HpmgNR-cES5Zu3n2tWrnGcA=w544-h544-l90-rj",
        "sabrina carpenter" to "https://lh3.googleusercontent.com/FMh1mOI0ufvUCAkbUM6aUmU5WK7O5PnndyyXKP1-DCEip20SQz5eeYn3lZ29-ASb-19ZfBVc_NKe5Ko=w544-h544-l90-rj",
        "olivia rodrigo" to "https://yt3.googleusercontent.com/41-4WZupE4yY88igineZefzBZ3ud2nrtlBMv61OBWOfOcATol8PhmI5OZ0fLlrTszyZ3Ul9I9sE=w544-h544-l90-rj",
        "the weeknd" to "https://lh3.googleusercontent.com/U-SAmNOu4TynE818gLCfKsuHZ0U5YNEtO9mrjSI9WCCKERs98LzrCal5kajBBTQNwdcisoB2Bn-pHp4=w544-h544-l90-rj",
        "ed sheeran" to "https://lh3.googleusercontent.com/jQoBIAS6JjFGpcqQY1M_Mh3AasOvFENCdVRxkgax1a0K6qiq7AgE3MbJ6Jtt-Jndcarvoawmrg66KTny=w544-h544-l90-rj",
        "ariana grande" to "https://yt3.googleusercontent.com/DU6Kpr5TYKcW6QHvMnsJau5_8QSuix8LCLtf5UEaziZZdXw8SxvcxJ9YWmVIQuzhg2R-MVHYgjdGCQ=w544-h544-l90-rj",
        "justin bieber" to "https://lh3.googleusercontent.com/4ULlRiFBFglNemZJyKn6_e2-iOIdJEbgBgq_79RQclndG6pge0yGgS2k2On6E1FkCJzenyHkHRzkvjFp=w544-h544-l90-rj",
        "dua lipa" to "https://lh3.googleusercontent.com/aFx8s1fTuelgxONGbezmTG0EKR8r82uB5H-Q6ZJtssyCWLJWF8GfZNr4tHo84sXdFCPBKrA4R6zXOss=w544-h544-l90-rj",
        "maroon 5" to "https://yt3.googleusercontent.com/7jndWy5UJwFJKueBCrizeFWOFwxKUOZZjRMIcPyRlv1X18tY9aJpkuSz_PpamnHVjbYV6s_oER8oi4g=w544-h544-l90-rj",
        "lady gaga" to "https://lh3.googleusercontent.com/umSdMueKqbLBTq0jhiEzh1F82_C8hRVzkw2ZsxOqEktWjNXOKSCMPMGiJtu73myxd995RC4BqKL5fAXM=w544-h544-l90-rj",
        "harry styles" to "https://lh3.googleusercontent.com/O6RvnokIzjsvwQzyRLLkKOx0mvxLXy1bEspu1YdG48jQ2s6o64wyr5Cd8MWjACGocpsl2zlN8iJoMg=w544-h544-l90-rj",
        "charlie puth" to "https://lh3.googleusercontent.com/Wqr_bvGshelPD-n5OohFA1Au6r130npkqQX3BS0bEb4l373-FVX9ZhkhWykNKdq3CrNCF6ubJNTLVwI=w544-h544-l90-rj",
        "queen" to "https://yt3.googleusercontent.com/_Qx50zW4-diO8SuAKhaccaNLyTvObgE-TQv1jYS4M9Kma6llb3s-IHpBBvuZAkPh88lHvXTQGaWS7aE=w544-h544-l90-rj",
        "the beatles" to "https://lh3.googleusercontent.com/z8KZsHNKS-O1qYVyKlSErT_RLMSMwVht89USvSdFAd0EoRlBOppi9DOdRkv609Ye_tfq_Wp8WwhVJbw=w544-h544-l90-rj"
    )

    private val dynamicMemoryCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    /**
     * Resolves the avatar URL for any artist name.
     */
    fun getVerifiedAvatarUrl(artistName: String): String? {
        val clean = artistName.trim().lowercase()
        if (dynamicMemoryCache.containsKey(clean)) {
            return dynamicMemoryCache[clean]
        }
        val resolved = MASTER_ARTIST_AVATARS[clean]
            ?: MASTER_ARTIST_AVATARS.entries.firstOrNull { clean.contains(it.key) || it.key.contains(clean) }?.value
        if (resolved != null) {
            dynamicMemoryCache[clean] = resolved
        }
        return resolved
    }

    /**
     * Caches dynamically discovered artist avatar URLs in memory.
     */
    fun cacheAvatar(artistName: String, avatarUrl: String) {
        if (avatarUrl.isNotBlank()) {
            dynamicMemoryCache[artistName.trim().lowercase()] = avatarUrl
        }
    }

    /**
     * Initial curated popular artists list containing balanced Indonesian and Western superstars.
     */
    fun getCuratedPopularArtists(): List<Artist> {
        return listOf(
            // Top Indonesian Stars
            Artist(id = "art_sheilaon7", name = "Sheila On 7", avatarUrl = getVerifiedAvatarUrl("Sheila On 7"), isVerified = true),
            Artist(id = "art_bernadya", name = "Bernadya", avatarUrl = getVerifiedAvatarUrl("Bernadya"), isVerified = true),
            Artist(id = "art_hindia", name = "Hindia", avatarUrl = getVerifiedAvatarUrl("Hindia"), isVerified = true),
            Artist(id = "art_tulus", name = "Tulus", avatarUrl = getVerifiedAvatarUrl("Tulus"), isVerified = true),
            Artist(id = "art_mahalini", name = "Mahalini", avatarUrl = getVerifiedAvatarUrl("Mahalini"), isVerified = true),
            Artist(id = "art_salpriadi", name = "Sal Priadi", avatarUrl = getVerifiedAvatarUrl("Sal Priadi"), isVerified = true),
            Artist(id = "art_niki", name = "NIKI", avatarUrl = getVerifiedAvatarUrl("NIKI"), isVerified = true),
            Artist(id = "art_juicyluicy", name = "Juicy Luicy", avatarUrl = getVerifiedAvatarUrl("Juicy Luicy"), isVerified = true),
            Artist(id = "art_nadin", name = "Nadin Amizah", avatarUrl = getVerifiedAvatarUrl("Nadin Amizah"), isVerified = true),
            Artist(id = "art_dewa19", name = "Dewa 19", avatarUrl = getVerifiedAvatarUrl("Dewa 19"), isVerified = true),
            Artist(id = "art_noah", name = "Noah", avatarUrl = getVerifiedAvatarUrl("Noah"), isVerified = true),
            Artist(id = "art_lyodra", name = "Lyodra", avatarUrl = getVerifiedAvatarUrl("Lyodra"), isVerified = true),
            Artist(id = "art_tiara", name = "Tiara Andini", avatarUrl = getVerifiedAvatarUrl("Tiara Andini"), isVerified = true),

            // Top Western Stars
            Artist(id = "art_brunomars", name = "Bruno Mars", avatarUrl = getVerifiedAvatarUrl("Bruno Mars"), isVerified = true),
            Artist(id = "art_taylorswift", name = "Taylor Swift", avatarUrl = getVerifiedAvatarUrl("Taylor Swift"), isVerified = true),
            Artist(id = "art_billieeilish", name = "Billie Eilish", avatarUrl = getVerifiedAvatarUrl("Billie Eilish"), isVerified = true),
            Artist(id = "art_coldplay", name = "Coldplay", avatarUrl = getVerifiedAvatarUrl("Coldplay"), isVerified = true),
            Artist(id = "art_sabrinacarpenter", name = "Sabrina Carpenter", avatarUrl = getVerifiedAvatarUrl("Sabrina Carpenter"), isVerified = true),
            Artist(id = "art_oliviarodrigo", name = "Olivia Rodrigo", avatarUrl = getVerifiedAvatarUrl("Olivia Rodrigo"), isVerified = true),
            Artist(id = "art_theweeknd", name = "The Weeknd", avatarUrl = getVerifiedAvatarUrl("The Weeknd"), isVerified = true),
            Artist(id = "art_edsheeran", name = "Ed Sheeran", avatarUrl = getVerifiedAvatarUrl("Ed Sheeran"), isVerified = true),
            Artist(id = "art_arianagrande", name = "Ariana Grande", avatarUrl = getVerifiedAvatarUrl("Ariana Grande"), isVerified = true),
            Artist(id = "art_dualipa", name = "Dua Lipa", avatarUrl = getVerifiedAvatarUrl("Dua Lipa"), isVerified = true),
            Artist(id = "art_bleachers", name = "Bleachers", avatarUrl = getVerifiedAvatarUrl("Bleachers"), isVerified = true)
        )
    }

    // Genre Clusters
    private val CLUSTER_INDO_INDIE_FOLK = listOf(
        "bernadya", "hindia", "sal priadi", "nadin amizah", "feby putri", "pamungkas",
        "danilla", "fiersa besari", "juicy luicy", "fourtwnty", "idgitaf", "dere", "eñau", "enau"
    )

    private val CLUSTER_INDO_POP_MAINSTREAM = listOf(
        "tulus", "mahalini", "lyodra", "tiara andini", "raisa", "afgan", "rizky febian",
        "yura yunita", "jaz", "budi doremi", "judika", "anji"
    )

    private val CLUSTER_INDO_BAND_NOSTALGIA = listOf(
        "sheila on 7", "dewa 19", "noah", "peterpan", "padi", "ungu", "d'masiv",
        "gigi", "ada band", "kotak", "letto", "maliq & d'essentials", "kahitna"
    )

    private val CLUSTER_WESTERN_POP_INDIE = listOf(
        "radiohead", "bruno mars", "taylor swift", "billie eilish", "coldplay", "sabrina carpenter",
        "olivia rodrigo", "the weeknd", "ed sheeran", "ariana grande", "justin bieber",
        "dua lipa", "maroon 5", "bleachers", "harry styles", "charlie puth", "shawn mendes", "lady gaga"
    )

    private val CLUSTER_INDO_DANGDUT_KOPLO = listOf(
        "denny caknan", "guyon waton", "happy asmara", "gilga sahid", "ndarboy genk",
        "shinta arsinta", "woro widowati", "difarina indra"
    )

    /**
     * Finds related peer artists with matching genre and musical vibe.
     */
    fun getPeerArtists(artistName: String): List<String> {
        val clean = artistName.trim().lowercase()

        val cluster = when {
            CLUSTER_INDO_INDIE_FOLK.any { clean.contains(it) || it.contains(clean) } -> CLUSTER_INDO_INDIE_FOLK
            CLUSTER_INDO_POP_MAINSTREAM.any { clean.contains(it) || it.contains(clean) } -> CLUSTER_INDO_POP_MAINSTREAM
            CLUSTER_INDO_BAND_NOSTALGIA.any { clean.contains(it) || it.contains(clean) } -> CLUSTER_INDO_BAND_NOSTALGIA
            CLUSTER_WESTERN_POP_INDIE.any { clean.contains(it) || it.contains(clean) } -> CLUSTER_WESTERN_POP_INDIE
            CLUSTER_INDO_DANGDUT_KOPLO.any { clean.contains(it) || it.contains(clean) } -> CLUSTER_INDO_DANGDUT_KOPLO
            else -> emptyList()
        }

        return cluster.filterNot { it.contains(clean) || clean.contains(it) }
    }

    /**
     * Determines whether a candidate track matches the genre/vibe of the seed artist,
     * preventing sudden jumps between opposing genres (e.g. western/indie pop jumping to dangdut).
     */
    fun isTrackCompatibleWithSeed(candidate: Track, seedArtist: String): Boolean {
        val seedClean = seedArtist.trim().lowercase()
        val candArtist = candidate.artist.name.trim().lowercase()
        val candTitle = candidate.title.trim().lowercase()

        // Same artist is always compatible
        if (candArtist.contains(seedClean) || seedClean.contains(candArtist)) return true

        val isSeedDangdut = CLUSTER_INDO_DANGDUT_KOPLO.any { seedClean.contains(it) }
        val isCandDangdut = CLUSTER_INDO_DANGDUT_KOPLO.any { candArtist.contains(it) } ||
                candTitle.contains("dangdut") || candTitle.contains("koplo") || candTitle.contains("kendang")

        // If seed is NOT dangdut, candidate MUST NOT be dangdut
        if (!isSeedDangdut && isCandDangdut) return false

        // If seed is Western Pop, candidate should ideally be Western Pop or Global Indie
        val isSeedWestern = CLUSTER_WESTERN_POP_INDIE.any { seedClean.contains(it) }
        if (isSeedWestern && isCandDangdut) return false

        return true
    }

    /**
     * Strict verification filter for Artist Detail screen.
     * Ensures ONLY tracks legitimately performed by the target artist or featuring the target artist
     * are shown in their "Lagu Populer" section.
     *
     * Rejects:
     * - Random tracks with titles that happen to mimic the artist's name (e.g. title: "Bernadya" by someone else)
     * - Cover singers or obscure artists mentioning the artist in the title
     */
    fun isLegitimateArtistTrack(track: Track, targetArtist: String): Boolean {
        val targetClean = targetArtist.trim().lowercase()
        val artistClean = track.artist.name.trim().lowercase()
        val titleClean = track.title.trim().lowercase()

        // 1. Direct or collaborator artist name match
        val isArtistDirect = artistClean == targetClean ||
                artistClean.startsWith("$targetClean ") ||
                artistClean.endsWith(" $targetClean") ||
                artistClean.contains("$targetClean &") ||
                artistClean.contains("& $targetClean") ||
                artistClean.contains("$targetClean,") ||
                artistClean.contains(", $targetClean") ||
                artistClean.contains("$targetClean feat") ||
                artistClean.contains("feat. $targetClean") ||
                artistClean.contains("feat $targetClean") ||
                artistClean.contains("ft. $targetClean") ||
                artistClean.contains("with $targetClean") ||
                artistClean.contains("x $targetClean") ||
                artistClean.contains("$targetClean x")

        if (isArtistDirect) {
            // Reject if the title clearly states it is an impersonator or unofficial cover
            if (titleClean.contains("cover by") || titleClean.contains("karaoke version") || titleClean.contains("tribute to")) {
                return false
            }
            return true
        }

        // 2. Official featured guest appearance in title (e.g. "Title (feat. TargetArtist)")
        // Crucial rule: The song title itself MUST NOT just be the artist's name!
        val isExplicitCollabInTitle = (
                titleClean.contains("feat. $targetClean") ||
                titleClean.contains("feat $targetClean") ||
                titleClean.contains("ft. $targetClean") ||
                titleClean.contains("ft $targetClean") ||
                titleClean.contains("with $targetClean") ||
                titleClean.contains("x $targetClean")
        ) && titleClean != targetClean && !titleClean.startsWith("$targetClean -")

        return isExplicitCollabInTitle
    }

    /**
     * Parses collaboration details from track title and artist name.
     * Returns a pair of (primaryArtist, listOfFeaturedArtists).
     * Handles:
     * - Title: "Song Title (feat. Artist B)"
     * - Title: "Song Title (ft. Artist B, Artist C)"
     * - Title: "Song Title [feat. Artist B]"
     * - Title: "Song Title with Artist B"
     * - Artist: "Artist A & Artist B"
     * - Artist: "Artist A, Artist B & Artist C"
     * - Artist: "Artist A x Artist B"
     * - Artist: "Artist A feat. Artist B"
     */
    fun parseCollaborators(rawTitle: String, rawArtist: String): Pair<String, List<String>> {
        val collaborators = mutableListOf<String>()
        var primaryArtist = rawArtist.trim()

        try {
            // 1. Extract from title: (feat. X), [ft. X], (with X)
            val featTitleRegex = "\\s*[({\\[]\\s*(?:feat\\.?|ft\\.?|featuring|with)\\s+([^()\\[\\]{}]+)[)}\\]]".toRegex(RegexOption.IGNORE_CASE)
            val titleMatch = featTitleRegex.find(rawTitle)
            if (titleMatch != null) {
                val collabStr = titleMatch.groupValues[1]
                val splitCollabs = collabStr.split(Regex(""",\s*|\s*&\s*|\s+and\s+|\s+x\s+""", RegexOption.IGNORE_CASE))
                for (c in splitCollabs) {
                    val cleanC = c.trim()
                    if (cleanC.isNotBlank() && !collaborators.contains(cleanC) && !cleanC.equals(primaryArtist, ignoreCase = true)) {
                        collaborators.add(cleanC)
                    }
                }
            }

            // 2. Extract from artist string if it contains multiple artists
            val featArtistRegex = Regex("""\s+(?:feat\.?|ft\.?|featuring|with)\s+""", RegexOption.IGNORE_CASE)
            if (featArtistRegex.containsMatchIn(primaryArtist)) {
                val parts = primaryArtist.split(featArtistRegex, limit = 2)
                primaryArtist = parts[0].trim()
                if (parts.size > 1) {
                    val splitCollabs = parts[1].split(Regex(""",\s*|\s*&\s*|\s+and\s+|\s+x\s+""", RegexOption.IGNORE_CASE))
                    for (c in splitCollabs) {
                        val cleanC = c.trim()
                        if (cleanC.isNotBlank() && !collaborators.contains(cleanC) && !cleanC.equals(primaryArtist, ignoreCase = true)) {
                            collaborators.add(cleanC)
                        }
                    }
                }
            } else if (primaryArtist.contains(" & ") || primaryArtist.contains(", ") || primaryArtist.contains(" x ") || primaryArtist.contains(" X ")) {
                val parts = primaryArtist.split(Regex(""",\s*|\s*&\s*|\s+x\s+|\s+X\s+"""))
                if (parts.isNotEmpty()) {
                    primaryArtist = parts[0].trim()
                    for (i in 1 until parts.size) {
                        val cleanC = parts[i].trim()
                        if (cleanC.isNotBlank() && !collaborators.contains(cleanC)) {
                            collaborators.add(cleanC)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return Pair(primaryArtist, collaborators.distinct())
    }
}
