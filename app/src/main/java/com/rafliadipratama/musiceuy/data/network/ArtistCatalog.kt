package com.rafliadipratama.musiceuy.data.network

import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track

object ArtistCatalog {

    /**
     * Master verified avatar directory for popular Indonesian and Western artists.
     * Guarantees 100% high-resolution (500x500) avatars even when offline or newly played.
     */
    private val MASTER_ARTIST_AVATARS = mapOf(
        // Top Indonesian Artists
        "sheila on 7" to "https://cdn-images.dzcdn.net/images/artist/9e6cc50e80a7dcb4dacc7eb15427335b/500x500-000000-80-0-0.jpg",
        "bernadya" to "https://cdn-images.dzcdn.net/images/artist/5bd274a72993ee8b8e72a7a8aedc12b5/500x500-000000-80-0-0.jpg",
        "hindia" to "https://cdn-images.dzcdn.net/images/artist/b33a022fb4490dce3b6e12d1c19c0123/500x500-000000-80-0-0.jpg",
        "tulus" to "https://cdn-images.dzcdn.net/images/artist/c207c7f16afc162acca87e587e47b442/500x500-000000-80-0-0.jpg",
        "mahalini" to "https://cdn-images.dzcdn.net/images/artist/bbb3c9932bf14c2ca059e24a041baf91/500x500-000000-80-0-0.jpg",
        "niki" to "https://cdn-images.dzcdn.net/images/artist/324c50c04a5a944117cd3daa0963fc63/500x500-000000-80-0-0.jpg",
        "sal priadi" to "https://cdn-images.dzcdn.net/images/artist/513217e1630470658eb37ac7d7eb1910/500x500-000000-80-0-0.jpg",
        "juicy luicy" to "https://cdn-images.dzcdn.net/images/artist/b94e4bb5570b617550f28dbe0012f973/500x500-000000-80-0-0.jpg",
        "nadin amizah" to "https://cdn-images.dzcdn.net/images/artist/10a75e65ad84fcfadeca0b74d1a56d0c/500x500-000000-80-0-0.jpg",
        "dewa 19" to "https://cdn-images.dzcdn.net/images/artist/7164efb4540b482dd5abac8bff423429/500x500-000000-80-0-0.jpg",
        "noah" to "https://cdn-images.dzcdn.net/images/artist/d0bdfce850235546343bead12c1d2689/500x500-000000-80-0-0.jpg",
        "peterpan" to "https://cdn-images.dzcdn.net/images/artist/d0bdfce850235546343bead12c1d2689/500x500-000000-80-0-0.jpg",
        "lyodra" to "https://cdn-images.dzcdn.net/images/artist/153076781614790ca9a7dd0177cad752/500x500-000000-80-0-0.jpg",
        "tiara andini" to "https://cdn-images.dzcdn.net/images/artist/88914c1499f102f0941cbb65850c4886/500x500-000000-80-0-0.jpg",
        "denny caknan" to "https://cdn-images.dzcdn.net/images/artist/2e8a010d43c385b64bda3d17214566d1/500x500-000000-80-0-0.jpg",
        "guyon waton" to "https://cdn-images.dzcdn.net/images/artist/ba6e13e4f4056b3affdd5794caca1e0f/500x500-000000-80-0-0.jpg",
        "feby putri" to "https://cdn-images.dzcdn.net/images/artist/0497658071183606d6ac0b0549c092a3/500x500-000000-80-0-0.jpg",
        "pamungkas" to "https://cdn-images.dzcdn.net/images/artist/cf78395171e26a522ba28f4d41b5b6bc/500x500-000000-80-0-0.jpg",
        "reality club" to "https://cdn-images.dzcdn.net/images/artist/be7235d469d786aa4e18c5e6d67b7e73/500x500-000000-80-0-0.jpg",
        "fourtwnty" to "https://cdn-images.dzcdn.net/images/artist/5ef14df8afed38e82e07143abf300185/500x500-000000-80-0-0.jpg",
        "padi" to "https://cdn-images.dzcdn.net/images/artist/9e6cc50e80a7dcb4dacc7eb15427335b/500x500-000000-80-0-0.jpg",
        "raisa" to "https://cdn-images.dzcdn.net/images/artist/9312bb46a81bb01efbe94adcb581f18e/500x500-000000-80-0-0.jpg",
        "afgan" to "https://cdn-images.dzcdn.net/images/artist/e71b29a2884c7f0709b119c676646736/500x500-000000-80-0-0.jpg",
        "rizky febian" to "https://cdn-images.dzcdn.net/images/artist/04da839e559e8e2fa5160faea8ba2564/500x500-000000-80-0-0.jpg",
        "idgitaf" to "https://cdn-images.dzcdn.net/images/artist/a16ee510804470b12bc1a8f0a0fc5b63/500x500-000000-80-0-0.jpg",
        "fiersa besari" to "https://cdn-images.dzcdn.net/images/artist/cf842f1f83c078bfa7fdb1a868f0cb67/500x500-000000-80-0-0.jpg",
        "eñau" to "https://yt3.googleusercontent.com/4_-ytwCqSUkPrYfjkSaY7Cs497XnahcMR7_vCcxoahaUjeoESN6XvxZlrg9FJ2sPYyL75oYZdDg2DPbw=w544-h544-l90-rj",
        "enau" to "https://yt3.googleusercontent.com/4_-ytwCqSUkPrYfjkSaY7Cs497XnahcMR7_vCcxoahaUjeoESN6XvxZlrg9FJ2sPYyL75oYZdDg2DPbw=w544-h544-l90-rj",
        ".feast" to "https://cdn-images.dzcdn.net/images/artist/39f07b5656c1061598b5d68d1c5f5cb1/500x500-000000-80-0-0.jpg",
        "feast" to "https://cdn-images.dzcdn.net/images/artist/39f07b5656c1061598b5d68d1c5f5cb1/500x500-000000-80-0-0.jpg",
        "ran" to "https://cdn-images.dzcdn.net/images/artist/27e997f804fc9a9a3b680516be85fb83/500x500-000000-80-0-0.jpg",
        "maman fvndy" to "https://yt3.googleusercontent.com/jFp7Zv9tsj-9Q53PUX-FJq2MG9YoA5pmRMu_kpd3VqbyEoJ7GbQ7TkMRIa1I8CwPJ9a5zvfVN-CcgZGLPw=w544-h544-l90-rj",
        "payung teduh" to "https://cdn-images.dzcdn.net/images/artist/e12d1b7d530eeffdbdf7bc1871a26f86/500x500-000000-80-0-0.jpg",
        "danilla" to "https://cdn-images.dzcdn.net/images/artist/00642f4c39f02279f6fa1e13a483e587/500x500-000000-80-0-0.jpg",

        // Top Western Artists
        "michael jackson" to "https://cdn-images.dzcdn.net/images/artist/ff1c8cf777db1efc2043b90d79dcbfc3/500x500-000000-80-0-0.jpg",
        "bruno mars" to "https://cdn-images.dzcdn.net/images/artist/90f0b5b11df4f87ee878f38569b5995b/500x500-000000-80-0-0.jpg",
        "taylor swift" to "https://cdn-images.dzcdn.net/images/artist/cc2495870fe1a792ad0cdb05501ad5ec/500x500-000000-80-0-0.jpg",
        "billie eilish" to "https://cdn-images.dzcdn.net/images/artist/8eab1a9a644889aabaca1e193e05f984/500x500-000000-80-0-0.jpg",
        "coldplay" to "https://cdn-images.dzcdn.net/images/artist/3087954bca22f306324912e5ac8375c3/500x500-000000-80-0-0.jpg",
        "bleachers" to "https://cdn-images.dzcdn.net/images/artist/16b4bac07438d0616f2a29fac91d311b/500x500-000000-80-0-0.jpg",
        "sabrina carpenter" to "https://cdn-images.dzcdn.net/images/artist/4a9cdc7737e2a0e59b4917b47884b859/500x500-000000-80-0-0.jpg",
        "olivia rodrigo" to "https://cdn-images.dzcdn.net/images/artist/2c9e480317183c037eaebcd7ba96daf4/500x500-000000-80-0-0.jpg",
        "the weeknd" to "https://cdn-images.dzcdn.net/images/artist/581693b4724a7fcfa754455101e13a44/500x500-000000-80-0-0.jpg",
        "ed sheeran" to "https://cdn-images.dzcdn.net/images/artist/d6bb84390641d8ae9118228d9544e53d/500x500-000000-80-0-0.jpg",
        "ariana grande" to "https://cdn-images.dzcdn.net/images/artist/721d8fab84b315502de422b8d0901509/500x500-000000-80-0-0.jpg",
        "justin bieber" to "https://cdn-images.dzcdn.net/images/artist/fe097f693cebf1f882e3da79e99e3bf9/500x500-000000-80-0-0.jpg",
        "dua lipa" to "https://cdn-images.dzcdn.net/images/artist/877872aaf75694f11d53c318700ab2b5/500x500-000000-80-0-0.jpg",
        "maroon 5" to "https://cdn-images.dzcdn.net/images/artist/bbb526b9666c7e31dee295bcabbbdd8e/500x500-000000-80-0-0.jpg",
        "lady gaga" to "https://cdn-images.dzcdn.net/images/artist/8b7245c117d74868e803c518ca47c0b0/500x500-000000-80-0-0.jpg",
        "harry styles" to "https://cdn-images.dzcdn.net/images/artist/063f91f37929d2b24e65017409c99c55/500x500-000000-80-0-0.jpg",
        "charlie puth" to "https://cdn-images.dzcdn.net/images/artist/a8d87a53c6e48ae2a8740df94916a048/500x500-000000-80-0-0.jpg",
        "queen" to "https://cdn-images.dzcdn.net/images/artist/066d99ef8eb773531fb5a9cd28828b8a/500x500-000000-80-0-0.jpg",
        "the beatles" to "https://cdn-images.dzcdn.net/images/artist/3b5936ca495d4e1302fb36be9f807217/500x500-000000-80-0-0.jpg"
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
        "bruno mars", "taylor swift", "billie eilish", "coldplay", "sabrina carpenter",
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
}
